# Import config and ini config
import configparser

config = configparser.ConfigParser()
config.read("config.ini")

# Set configurble variables
file_size_limit = int(config["DEFAULT"]["file_size_limit"])

# Import 3rd part modules
# import setup
import os

# Set ffmpeg path
ffmpegpath = "/opt/python/ffmpeg_compiled/"
os.environ["PATH"] += os.pathsep + ffmpegpath

import sys
import json
import datetime
from pytz import timezone
from collections import Counter
import boto3
from botocore.exceptions import ClientError
from boto3.dynamodb.conditions import Key, Attr
import uuid
from urllib.parse import unquote_plus
import shutil
from decimal import getcontext, Decimal
import math
import time
import pandas as pd
import numpy as np

# Import Collector
import pycollector
from pycollector.project import Project
from pycollector.video import Video
import pycollector.admin.globals
from pycollector.admin.dashboard import Collector
from pycollector.admin.workforce import Collectors
from pycollector.admin.dropbox import Dropbox
from pycollector.admin.workforce import Collectors
from collector.admin import Video as VideoAdmin

# Set to target ENV
backend = pycollector.admin.globals.backend(env="prod", org="visym")

# Import internal modules
from util import *

import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)


s3_client = boto3.client("s3")
s3_resource = boto3.resource("s3")
dynamodb_resource = boto3.resource("dynamodb")

video_filetype = ".mp4"
annotation_filetype = ".json"


def inject_milliseconds_to_iso_timestamp(timestamp_str, delta):
    tmp_datetime = datetime.datetime.strptime(timestamp_str, "%Y-%m-%dT%H:%M:%S.%f%z")
    update_datetime = tmp_datetime + datetime.timedelta(milliseconds=500 + delta)
    return time_format(update_datetime)


def get_file_creating_datetime(json_obj_key):
    """
    Get the file creation timestamp from the json object pathh in S3. The returning timestamp will be in ISO format and in Eastern Time
    """
    response = s3_client.list_objects_v2(Bucket=backend.s3_bucket(), Prefix=json_obj_key)
    return time_format(response["Contents"][0]["LastModified"].astimezone(timezone("US/Eastern")))


def Publish_SNS(subject="[Testing]Exception while proccessing...", error_message=None):
    """
    Send a message to the topic
    """

    sns = boto3.client("sns")
    subject = subject
    message = "Error message: {0} ".format(error_message)
    sns.publish(
        TargetArn=backend.aws_sns_topic_arn(),
        Subject=subject,
        Message=message,
    )
    return


def check_object(bucket, key):
    """ Check if object exist"""
    response = s3_client.list_objects(Bucket=bucket, Prefix=key)
    return response


def get_collector_email(collector_id=None):
    """Get collectors' emails"""

    kce = Key("collector_id").eq(collector_id)

    collectors_table = backend.ddb_collector()

    response = collectors_table.query(
        KeyConditionExpression=kce,
    )

    return list(pd.DataFrame(response["Items"])["collector_email"])[0]


def update_collector_table(metadata):

    # Update data to Collector Table by collector ID
    collectors_table = backend.ddb_collector()

    try:
        collector_response = collectors_table.query(KeyConditionExpression=Key("collector_id").eq(metadata["collector_id"]))
    except ClientError as e:
        logger.warning("Not able to update collectors table in DDB:")
        logger.warning(e.response["Error"]["Message"])

    collector_update_response = collectors_table.update_item(
        Key={
            "collector_id": collector_response["Items"][0]["collector_id"],
            "collector_email": collector_response["Items"][0]["collector_email"],
        },
        UpdateExpression="set updatedAt = :updatedAt, uploaded_count = if_not_exists(uploaded_count, :start) + :increment, not_verified_count = if_not_exists(not_verified_count, :start) + :increment ",
        ExpressionAttributeValues={
            ":start": 0,
            ":increment": 1,
            ":updatedAt": datetime.datetime.now(timezone("US/Eastern")).isoformat(),
        },
        ReturnValues="UPDATED_NEW",
    )


def getAnimatedQuicklook(input_video_path, input_annotation_path):

    # JEBYRNE: mindim=1024
    v = Video(mp4file=input_video_path, jsonfile=input_annotation_path, mindim=512, dt=1)

    filename = v._filename.replace(video_filetype, "")

    # Get start_end_frames
    start_end_frames = [(value._startframe, value._endframe) for k, value in v._activities.items()]

    # Generateing
    logger.info("Generating animated quicklooks... ")

    if len(v.activities()) == 0:
        return [
            a.mindim(128).quicklook(n=9, animate=True, dt=30).webp("{0}_quicklook_{1}.webp".format(filename, 0), pause=2, smallest=True)
            for a in v.tracksplit()
        ]
    elif len(v.activities()) > 6:
        return []
    else:
        return [
            a.mindim(128)
            .quicklook(n=9, context=True, animate=True, dt=30)
            .webp("{0}_quicklook_{1}.webp".format(filename, idx), pause=2, smallest=True)
            for idx, a in enumerate(v.activityclip())
        ]


def getActivityMetaData(input_video_path, input_annotation_path):

    # JEBYRNE: mindim=1024
    v = Video(mp4file=input_video_path, jsonfile=input_annotation_path, mindim=512, dt=1)

    filename = v._filename.replace(video_filetype, "")

    # Variables for thumbnaiils
    thumbnail_file_name = filename + "_thumbnail"
    thumbnail_file_path = thumbnail_file_name + ".jpg"
    thumbnail_small_file_path = thumbnail_file_name + "_small" + ".jpg"

    if len(v.activities()) == 0:
        try:
            ################################################################################################################################################
            #
            # # Start of vipy version > 1.9.5, use the following line will be much more efficent
            # assert vipy.version.is_at_least('1.9.5')
            # a.thumbnail(frame=0).centersquare().maxdim(512).centercrop(256, 512).savefig(dpi=200).rgb().saveas(thumbnail_file_path)
            #
            ################################################################################################################################################
            v.clone().clip(0, 30).centersquare().maxdim(512).centercrop((256, 512)).load().frame(0).savefig(dpi=200).rgb().saveas(thumbnail_file_path)
            v.clone().clip(0, 30).centersquare().maxdim(512).centercrop((256, 512)).load().frame(0).savefig(dpi=72).rgb().saveas(
                thumbnail_small_file_path
            )
        except Exception as e:
            logger.warning(
                "Not able to generate thumbnail for video file {0} and JSON file {1} due to exception: {2} ".format(
                    input_video_path, input_annotation_path, e
                )
            )
            thumbnail_file_path = "None"

        quicklook_file_paths = [a.quicklook(n=9, context=True).saveas("{0}_quicklook_{1}.jpg".format(filename, 0)) for a in v.tracksplit()]
        activity_list = [t.category() for a in v.tracksplit() for t in a.tracklist()]
        shortlabels_list = [t.shortlabel() for a in v.tracksplit() for t in a.tracklist()]
        instance_bghashes = []
        start_end_frames = [(track._startframe, track._endframe) for track in v.tracksplit()]

    else:
        # Get activity list
        activity_list = [value.category() for k, value in v._activities.items()]

        # Get short label list
        shortlabels_list = [value.shortlabel() for k, value in v._activities.items()]

        # Get start_end_frames
        start_end_frames = [(value._startframe, value._endframe) for k, value in v._activities.items()]

        # Set up the empty list to store the results
        (quicklook_file_paths, instance_bghashes) = ([], [])

        # Generateing thumbnail from activities
        for idx, a in enumerate(v.activityclip()):
            if idx == 0:
                try:
                    a.clone().clip(0, 30).centersquare().maxdim(512).centercrop((256, 512)).load().frame(0).savefig(dpi=200).rgb().saveas(
                        thumbnail_file_path
                    )
                    a.clone().clip(0, 30).centersquare().maxdim(512).centercrop((256, 512)).load().frame(0).savefig(dpi=72).rgb().saveas(
                        thumbnail_small_file_path
                    )
                except Exception as e:
                    logger.warning(
                        "Not able to generate thumbnail for video file {0} and JSON file {1} due to exception: {2} ".format(
                            input_video_path, input_annotation_path, e
                        )
                    )
                    thumbnail_file_path = "None"

            # Generating quicklooks
            try:
                quicklook_file_paths.append(a.quicklook(n=9, context=True).saveas("{0}_quicklook_{1}.jpg".format(filename, idx)))
            except Exception as e:
                logger.warning(
                    "Not able to generate quicklook for video file {0} and JSON file {1} due to exception: {2} ".format(
                        input_video_path, input_annotation_path, e
                    )
                )
                return None, None, None, None, None, None, None

            # Generating background hashes
            try:
                instance_bghashes.append(a.frame(0).bghash())
            except Exception as e:
                logger.warning(
                    "Not able to generate animated quicklook for video file {0} and JSON file {1} due to exception: {2} ".format(
                        input_video_path, input_annotation_path, e
                    )
                )
                return None, None, None, None, None, None, None

            a.flush()

    print(
        quicklook_file_paths,
        activity_list,
        shortlabels_list,
        instance_bghashes,
        start_end_frames,
        thumbnail_file_path,
        thumbnail_small_file_path,
    )

    return (
        quicklook_file_paths,
        activity_list,
        shortlabels_list,
        instance_bghashes,
        start_end_frames,
        thumbnail_file_path,
        thumbnail_small_file_path,
    )


def check_and_download_object(bucket, key, filename, tries=3, sleep_secs=20):
    """ Check and download existing object """

    try:
        s3_resource.Object(bucket, key).load()
    except ClientError as e:

        logger.info("Unable to donwload file with key ".format(key))
        logger.info("bucket: ".format(bucket))
        logger.info("Tried Error: {0}".format(e))

        # Remove the hanginging mp4 file
        local_file_path = "/tmp/{}".format(filename)
        os.remove(local_file_path.replace("json", "mp4"))

        return int(e.response["Error"]["Code"]) != 404

    local_file_path = "/tmp/{}".format(filename)

    s3_resource.meta.client.download_file(bucket, key, local_file_path)

    return local_file_path  # To return as a json object -   json.loads(s3_client.get_object(Bucket=bucket, Key=key)['Body'].read().decode('utf-8')) - ### Read in JSON file https://medium.com/faun/parsing-a-json-file-from-a-s3-bucket-dane-fetterman-a0e0566d5c32


def extract_metadata_from_annotation(input_annotation_path):

    with open(input_annotation_path) as json_file:
        annotation = json.load(json_file)

    logger.info("Input Annotation: {0}".format(annotation))

    video_id = annotation["metadata"]["video_id"]
    collector_id = annotation["metadata"]["collector_id"]
    program_name = annotation["metadata"]["program_name"]
    project_id = annotation["metadata"]["project_id"]
    project_name = annotation["metadata"]["project_name"]
    collection_id = annotation["metadata"]["collection_id"]
    collection_name = annotation["metadata"]["collection_name"]
    subject_ids = [subject for subject in annotation["metadata"]["subject_ids"]]

    # Get Collector_email by collector_id
    collector_email = get_collector_email(collector_id)

    if "consent_video_json_url" in annotation["metadata"]:
        consent_video_json_url = annotation["metadata"]["consent_video_json_url"]
    else:
        consent_video_json_url = None

    duration = annotation["metadata"]["duration"]
    blurred_faces = annotation["metadata"]["blurred_faces"]
    collected_date_str = annotation["metadata"]["collected_date"]

    try:
        collected_date = datetime.datetime.strptime(collected_date_str, "%Y-%m-%d %H:%M:%S %z")  # iOS 1.0 (6)
    except:
        try:
            collected_date = datetime.datetime.strptime(collected_date_str, "%Y-%m-%d %I:%M:%S %p %z")  # bug number 55
        except:
            collected_date = datetime.datetime.strptime(collected_date_str, "%Y-%m-%dT%H:%M:%S%z")  # android 1.1.1 (3)

    collected_date = collected_date.strftime("%Y-%m-%d %H:%M:%S")  # canonicalize

    orientation = annotation["metadata"]["orientation"]
    frame_rate = annotation["metadata"]["frame_rate"]
    frame_width = annotation["metadata"]["frame_width"]
    frame_height = annotation["metadata"]["frame_height"]
    num_instances = len(annotation["activity"])
    activity_list = [activity["label"] for activity in annotation["activity"]]
    # activity_list_counts = Counter(activity_list)

    return {
        "video_id": video_id,
        "collector_id": collector_id,
        "collector_email": collector_email,
        "subject_ids": subject_ids,
        "program_name": program_name,
        "project_id": project_id,
        "project_name": project_name,
        "collection_id": collection_id,
        "collection_name": collection_name,
        "consent_video_json_url": consent_video_json_url,
        "duration": duration,
        "blurred_faces": blurred_faces,
        "collected_date": collected_date,
        "orientation": orientation,
        "frame_rate": frame_rate,
        "frame_width": frame_width,
        "frame_height": frame_height,
        "num_instances": num_instances,
        "activity_list": activity_list,
        #  'activity_list_counts' : activity_list_counts
    }


def save_to_videos_table(metadata, Raw_Video_Path, Annotation_Path, thumbnail_url, thumbnai_smalll_url, week_start, video_uploaded_datetime):

    videos_table = backend.ddb_video()

    # TODO Temp remedation for not having the consent video
    if metadata["consent_video_json_url"] == "":
        metadata["consent_video_json_url"] = "None"

    # Check if video_id in the metadata is matching with the actual file name in the  Raw_Video_Path. If not change it to match with the file name.
    video_id = metadata["video_id"]
    video_id_from_file = Raw_Video_Path.split("/")[-1].split(".")[0]
    if metadata["video_id"] != video_id_from_file:
        video_id = video_id_from_file

    video_item = {
        "id": metadata["collection_name"] + "_" + metadata["project_name"] + "_" + metadata["collector_id"],
        "video_id": video_id,
        "key": metadata["collection_name"] + "_" + metadata["project_name"] + "_" + metadata["collector_id"],
        "video_id": video_id,
        "__typename": backend.environment_name(),
        "query_attribute": "1",
        "collector_id": metadata["collector_id"],
        "collector_email": metadata["collector_email"],
        "program_id": metadata["program_name"],
        "program_name": metadata["program_name"],
        "project_id": metadata["project_id"],
        "project_name": metadata["project_name"],
        "collection_id": metadata["collection_id"],
        "collection_name": metadata["collection_name"],
        "subject_id": metadata["subject_ids"],
        "activities_list": metadata["activity_list"],
        "raw_video_file_path": Raw_Video_Path,
        "annotation_file_path": Annotation_Path,
        "thumbnail": thumbnail_url,
        "thumbnail_small": thumbnai_smalll_url,
        "processed_video_file_path": "None",
        "consent_video_json_url": metadata["consent_video_json_url"],
        "duration": Decimal(str(metadata["duration"])),
        # 'Object_Label_List' : ['None'],
        "blurred_faces": metadata["blurred_faces"],
        "collected_date": metadata["collected_date"],
        "orientation": metadata["orientation"],
        "frame_rate": Decimal(str(metadata["frame_rate"])),
        "frame_width": metadata["frame_width"],
        "frame_height": metadata["frame_height"],
        "num_instances": metadata["num_instances"],
        "rating_score": 0,
        "bad_box_score": 0,
        "bad_box_big_score": 0,
        "bad_box_small_score": 0,
        "bad_label_score": 0,
        "bad_timing_score": 0,
        "bad_viewpoint_score": 0,
        "bad_alignment_score": 0,
        "bad_video_score": 0,
        "bad_diversity_score": 0,
        "bad_visibility_score": 0,
        "good_for_training_score": 0,
        "review_reason": "None",
        "week": week_start,
        "verified": False,
        "video_state": "Collected",
        "uploaded_date": metadata["uploaded_date"],
        "video_sharing_link": metadata["dropbox_video_url"],
        "json_sharing_link": metadata["dropbox_json_url"],
        "updated_datetime": video_uploaded_datetime,
    }

    # Videos from practice projects will mark as junk videos immediately
    if "Practice" in metadata["project_name"]:
        video_item["bad_video_score"] = 1
        video_item["verified"] = True

    try:
        response = videos_table.put_item(
            Item=video_item,
            ConditionExpression="attribute_not_exists(symbol) AND attribute_not_exists(headline)",
        )

    except ClientError as e:
        # Ignore the ConditionalCheckFailedException, bubble up
        # other exceptions.
        if e.response["Error"]["Code"] != "ConditionalCheckFailedException":
            Publish_SNS(
                subject="AWS Notification Message - Failed to process video_id {0}".format(metadata["video_id"]),
                error_message="Not able to parse video {0} due to exception: {1} ".format(metadata["video_id"], e),
            )
            raise


def save_to_quicklooks_table(
    metadata, quicklook_s3_links, animated_quicklook_s3_links, instance_bghashes, start_end_frames, week_start, video_uploaded_datetime
):

    quicklooks_table = backend.ddb_instance()

    logger.info("Uploading file to Instances_table")

    for idx, activity in enumerate(metadata["activity_list"]):
        quicklooks_item = {
            "id": metadata["program_name"] + "_" + metadata["project_name"] + "_" + metadata["collection_name"] + "_" + activity,
            "video_id": metadata["video_id"],
            "collector_id": metadata["collector_id"],
            "collector_email": metadata["collector_email"],
            "project_name": metadata["project_name"],
            "project_id": metadata["project_id"],
            "project_name": metadata["project_name"],
            "collection_id": metadata["collection_id"],
            "collection_name": metadata["collection_name"],
            "subject_id": metadata["subject_ids"],
            "activity_name": activity,
            "shortlabel": metadata["shortlabels_list"][idx] if len(metadata["shortlabels_list"]) > 0 else None,
            "background_hash": instance_bghashes[idx] if len(instance_bghashes) > 0 else None,
            "start_frame": start_end_frames[idx][0],
            "end_frame": start_end_frames[idx][1],
            "start_frame_off_set": 0,
            "end_frame_off_set": 0,
            "bad_box_score": 0,
            "bad_box_big_score": 0,
            "bad_box_small_score": 0,
            "bad_label_score": 0,
            "bad_timing_score": 0,
            "bad_viewpoint_score": 0,
            "bad_alignment_score": 0,
            "bad_video_score": 0,
            "bad_diversity_score": 0,
            "bad_visibility_score": 0,
            "good_for_training_score": 0,
            "week": week_start,
            "verified": False,
            "s3_path": quicklook_s3_links[idx] if len(quicklook_s3_links) > 0 else None,
            "animation_s3_path": animated_quicklook_s3_links[idx] if len(animated_quicklook_s3_links) > 0 else None,
            "instance_id": "_".join([metadata["video_id"], str(idx)]),
            "rating_score": 0,
            "rating_score_finalized": False,
            "review_reason": "None",
            "program_id": metadata["program_name"],
            "instance_state": "Collected",
            "uploaded_date": inject_milliseconds_to_iso_timestamp(metadata["uploaded_date"], idx),
            "updated_datetime": datetime.datetime.now(timezone("US/Eastern")).isoformat(),
            "video_uploaded_datetime": video_uploaded_datetime,
        }

        if "Practice" in metadata["project_name"]:
            quicklooks_item["bad_video_score"] = 1
            quicklooks_item["verified"] = True

        try:
            response = quicklooks_table.put_item(
                Item=quicklooks_item,
                ConditionExpression="attribute_not_exists(symbol) AND attribute_not_exists(headline)",
            )
        except ClientError as e:
            if e.response["Error"]["Code"] != "ConditionalCheckFailedException":
                Publish_SNS(
                    subject="AWS Notification Message - Failed to save metadata into Instances table for video: {0} ".format(metadata["video_id"]),
                    error_message="Not able to parse video {0} due to exception: {1} ".format(metadata["video_id"], e),
                )
                raise

    logger.info("Doen uploading file to Instances_table")


def handler(event, context):

    # Set start time
    lambda_start_time = datetime.datetime.now(timezone("US/Eastern"))
    logger.info("Lambda execution starts at: {0}".format(lambda_start_time.isoformat()))
    logger.info("With event: {0}".format(event))

    for record in event["Records"]:
        bucket = record["s3"]["bucket"]["name"]
        key = unquote_plus(record["s3"]["object"]["key"])
        this_filename = key.split("/")[-1]
        input_video_path = "/tmp/{}".format(this_filename)

        logger.info("Proccessing file: {0}".format(this_filename))

        # Check disk storage usage in /tmp
        logger.info("Disk storage usage in /tmp: ")
        os.system("du -h -d1 /tmp")

        # Check os disk storage usag
        logger.info("Disk Usage: ")
        os.system("df -hi")

        # Set video uplaoded datetime
        video_uploaded_datetime = datetime.datetime.now(timezone("US/Eastern")).isoformat()

        # Check if this is edit video
        is_edited = False
        if "is_edited" in record:
            is_edited = True
            video_uploaded_datetime = record["video_uploaded_datetime"]

        video_obj_key = ""
        json_obj_key = ""

        # Check if JSON or Video files are ready
        if video_filetype in key:
            file_check_result = check_object(bucket, key.replace(video_filetype, annotation_filetype))
            video_obj_key = key
            json_obj_key = key.replace(video_filetype, annotation_filetype)

            logger.info("File check result {0}".format(file_check_result))

            if "Contents" not in file_check_result:
                logger.info("File {0} is not ready yet ".format(key.replace(video_filetype, annotation_filetype)))
                logger.info("Done for Proccessing file: {0}".format(this_filename))
                continue
        elif annotation_filetype in key:
            file_check_result = check_object(bucket, key.replace(annotation_filetype, video_filetype))
            json_obj_key = key
            video_obj_key = key.replace(annotation_filetype, video_filetype)
            input_video_path = input_video_path.replace(annotation_filetype, video_filetype)

            logger.info("File check result {0}".format(file_check_result))

            if "Contents" not in file_check_result:
                logger.info("File {0} is not ready yet ".format(key.replace(annotation_filetype, video_filetype)))
                logger.info("Done for Proccessing file: ".format(this_filename))
                continue

        try:
            s3_client.download_file(bucket, video_obj_key, input_video_path)
        except Exception as e:
            logger.info("Exception: ".format(e))
            logger.info("List tmpdir")
            tmpdir = "/tmp"
            print(os.listdir(tmpdir))

            file_list = os.listdir(tmpdir)
            file_size_pairs = []
            for file in file_list:

                # Use join to get full file path.
                location = os.path.join(tmpdir, file)

                # Get size and add to list of tuples.
                size = os.path.getsize(location)
                file_size_pairs.append((size, file))
            # Sort list of tuples by the first element, size.
            file_size_pairs.sort(key=lambda s: s[0])

            # Display pairs.
            for pair in file_size_pairs:
                logger.info(pair)

            Publish_SNS(
                subject="AWS Notification Message - Not able to download file: {0} ".format(video_obj_key),
                error_message="Not able to download files video {0} due to exception: {1} ".format(video_obj_key, e),
            )
            raise Exception("Not able to download files due to exception: ", e)

        # Try to parse the JSON file
        try:
            # check if the JSON file is already exist. Assuming the JSON file should be uploaded before the video due to file size
            input_annotation_path = check_and_download_object(
                bucket,
                json_obj_key,
                this_filename.replace(video_filetype, annotation_filetype),
            )
        except Exception as e:
            logger.warning("Not able to parse JSON file {0} due to exception: {1} ".format(json_obj_key, e))
            logger.warning("Removing cached video and JSON files for Video_ID...")
            # Remove the video and annotation files
            os.remove(input_video_path)
            os.remove(input_annotation_path)
            Publish_SNS(
                subject="AWS Notification Message - Failed to process uploaded file {0}".format(this_filename),
                error_message="Not able to parse JSON file {0} due to exception: {1} ".format(json_obj_key, e),
            )
            continue

        # Try to parse the JSON file
        try:
            metadata = extract_metadata_from_annotation(input_annotation_path)
        except Exception as e:
            logger.warning("Not able to extract metadata from JSON file {0} due to exception: {1} ".format(json_obj_key, e))
            logger.warning("Removing cached video and JSON files for Video_ID...")
            # Remove the video and annotation files
            os.remove(input_video_path)
            os.remove(input_annotation_path)
            # raise Exception("Not able to parse JSON file {0} due to exception: {1} ".format(json_obj_key, e))
            # TODO - raise exception by push notification
            Publish_SNS(
                subject="AWS Notification Message - Failed to process uploaded file {0}".format(this_filename),
                error_message="Not able to parse JSON file {0} due to exception: {1} ".format(json_obj_key, e),
            )
            continue

        # Check if the video is already saved
        videoClient = VideoAdmin()

        isNewVideo = True

        try:
            uploaded_date = videoClient.get_sort_key_by_video_id(metadata["video_id"])
            uploaded_date_dt = datetime.datetime.strptime(uploaded_date, "%Y-%m-%dT%H:%M:%S.%f%z")
            week_start = (uploaded_date_dt.date() - datetime.timedelta(days=uploaded_date_dt.date().weekday())).isoformat()
            logger.info("Video already saved in database.")
            isNewVideo = False
        except:
            # Get original creation datetime
            uploaded_date = get_file_creating_datetime(json_obj_key)
            uploaded_date_dt = datetime.datetime.strptime(uploaded_date, "%Y-%m-%dT%H:%M:%S.%f%z")
            week_start = (uploaded_date_dt.date() - datetime.timedelta(days=uploaded_date_dt.date().weekday())).isoformat()
            logger.info("This Video is new. Saving to database")

        # Check file size
        if ("is_edited" not in record) and (record["s3"]["object"]["size"] > file_size_limit):
            logger.warning(
                "Not able to proccess video file {0} due to file size exceeded 100mb. This file is {1}mb".format(
                    video_obj_key, record["s3"]["object"]["size"] / 1000000
                )
            )
            logger.warning("Removing cached video and JSON files for Video_ID...")
            # Remove the video and annotation files
            os.remove(input_video_path)
            os.remove(input_annotation_path)
            # raise Exception("Not able to parse JSON file {0} due to exception: {1} ".format(json_obj_key, e))
            Publish_SNS(
                subject="AWS Notification Message - Failed to process uploaded file {0}".format(this_filename),
                error_message="Not able to proccess video file {0} due to file size too big. Here is the metadata from the JSON file: {1} ".format(
                    video_obj_key, metadata
                ),
            )
            continue

        # Create quicklooks
        (
            quicklook_file_paths,
            activity_list,
            shortlabels_list,
            instance_bghashes,
            start_end_frames,
            thumbnail_file_path,
            thumbnail_small_file_path,
        ) = getActivityMetaData(input_video_path, input_annotation_path)

        # Check if there is any exception during getActivityMetaData
        if quicklook_file_paths == None:
            return {
                "statusCode": 400,
                "body": json.dumps("Could not proccesse file: {0}".format(this_filename)),
            }

        quicklook_s3_links = []
        animated_quicklook_s3_links = []

        metadata["shortlabels_list"] = shortlabels_list
        metadata["activity_list"] = activity_list
        metadata["activity_list_counts"] = Counter(activity_list)
        metadata["start_end_frames"] = start_end_frames
        metadata["uploaded_date"] = uploaded_date

        logger.info("metadata:  {0}".format(metadata))

        public_bucketname = "visym-public-data224027-visymcodev"
        public_s3_url_template = "https://{0}.s3.amazonaws.com/{1}"

        # Upload thumbnails to S3 and return S3 thumbnail urls
        # TODO - set MEVA to default program ID for now
        program_name = metadata["program_name"]

        thumbnail_filename = (
            program_name + "_" + metadata["project_id"] + "_" + metadata["collection_id"] + "_" + metadata["video_id"] + "_thumbnail.jpg"
        )
        thumbnail_small_filename = (
            program_name + "_" + metadata["project_id"] + "_" + metadata["collection_id"] + "_" + metadata["video_id"] + "_thumbnail_small.jpg"
        )
        thumbnail_key = "".join(["Collections_Instances_Thumbnails/", thumbnail_filename])
        thumbnail_small_key = "".join(["Collections_Instances_Thumbnails/", thumbnail_small_filename])

        thumbnail_url = public_s3_url_template.format(public_bucketname, thumbnail_key)
        thumbnail_small_url = public_s3_url_template.format(public_bucketname, thumbnail_small_key)

        logger.info("thumbnail_small_url: {0}".format(thumbnail_small_url))

        if thumbnail_file_path == "None":
            thumbnail_key = "None"
            thumbnail_small_key = "None"
        else:
            s3_client.upload_file(
                thumbnail_file_path,
                public_bucketname,
                thumbnail_key,
                ExtraArgs={"ContentType": "image/jpeg"},
            )
            s3_client.upload_file(
                thumbnail_small_file_path,
                public_bucketname,
                thumbnail_small_key,
                ExtraArgs={"ContentType": "image/jpeg"},
            )

            logger.info("Remove thumbnail_files...")

            # Remove quicklooks image file
            os.remove(thumbnail_file_path)
            os.remove(thumbnail_small_file_path)

        for quicklook_file in quicklook_file_paths:

            quicklook_filename = quicklook_file.split("/")[-1]
            quicklook_key = "".join(["Quicklooks/", quicklook_filename])

            try:
                s3_client.upload_file(
                    quicklook_file,
                    public_bucketname,
                    quicklook_key,
                    ExtraArgs={"ContentType": "image/jpeg"},
                )

                logger.info("Remove quicklook_file: {0}".format(quicklook_file))

                # Remove quicklooks image file
                os.remove(quicklook_file)

                # Save quicklook links
                quicklook_s3_links.append(public_s3_url_template.format(public_bucketname, quicklook_key))

            except Exception as e:
                Publish_SNS(
                    subject="AWS Notification Message - Failed to process uploaded file {0}".format(this_filename),
                    error_message="Not able to upload quicklook file to s3 due to exception {0}".format(e),
                )
                return False

        # Store videos and json to dropbox
        t = Collectors().dropbox_token(email=metadata["collector_email"])

        metadata["dropbox_video_url"] = None
        metadata["dropbox_json_url"] = None

        if t is not None:
            d = Dropbox(token=t)

            try:
                d.upload(input_video_path)
                metadata["dropbox_video_url"] = d.link(input_video_path)
            except Exception as e:
                logger.info("Not able to upload video file to dropbox due to: {0}".format(e))

            # augmented input_annotation_path with the appended DATETIME string
            augmented_input_annotation_path = input_annotation_path.replace(".json", "_{:%Y%m%d_%H%M%S}".format(datetime.datetime.now()) + ".json")
            shutil.copy(input_annotation_path, augmented_input_annotation_path)

            try:
                d.upload(augmented_input_annotation_path)
                metadata["dropbox_json_url"] = d.link(augmented_input_annotation_path)
            except Exception as e:
                logger.info("Not able to upload annotation file to dropbox due to: {0}".format(e))

        Raw_Video_Path = "/".join([video_obj_key])
        Annotation_Path = "/".join([json_obj_key])

        if not is_edited:
            save_to_videos_table(metadata, Raw_Video_Path, Annotation_Path, thumbnail_url, thumbnail_small_url, week_start, video_uploaded_datetime)

        animated_quicklook_file_paths = getAnimatedQuicklook(input_video_path, input_annotation_path)

        # Remove the video and annotation files
        os.remove(input_video_path)
        os.remove(input_annotation_path)

        if len(animated_quicklook_file_paths) > 0:

            print(" len(animated_quicklook_file_paths): ", len(animated_quicklook_file_paths))

            logger.info(" len(animated_quicklook_file_paths): {0}".format(len(animated_quicklook_file_paths)))
            logger.info("animated_quicklook_file_paths: {0}".format(animated_quicklook_file_paths))
            for quicklook_file in animated_quicklook_file_paths:

                quicklook_filename = quicklook_file.split("/")[-1]
                quicklook_key = "".join(["Quicklooks/", quicklook_filename])

                try:
                    s3_client.upload_file(
                        quicklook_file,
                        public_bucketname,
                        quicklook_key,
                        ExtraArgs={"ContentType": "image/webp"},
                    )

                    logger.info("Remove animated quicklook_file: {0}".format(quicklook_file))

                    # Remove quicklooks image file
                    os.remove(quicklook_file)

                    # Save quicklook links
                    animated_quicklook_s3_links.append(public_s3_url_template.format(public_bucketname, quicklook_key))

                except Exception as e:
                    Publish_SNS(
                        subject="AWS Notification Message - Failed to process uploaded file {0}".format(this_filename),
                        error_message="Not able to upload animated quicklook file to s3 due to exception {0}".format(e),
                    )
                    return False

        save_to_quicklooks_table(
            metadata, quicklook_s3_links, animated_quicklook_s3_links, instance_bghashes, start_end_frames, week_start, video_uploaded_datetime
        )

        if isNewVideo:
            update_collector_table(metadata)

    return {
        "statusCode": 200,
        "body": json.dumps("Successfully proccessed file: {0}".format(this_filename)),
    }

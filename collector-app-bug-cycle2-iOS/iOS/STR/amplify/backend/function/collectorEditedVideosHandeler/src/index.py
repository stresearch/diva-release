from urllib.parse import unquote_plus
import os
import json
import boto3
import botocore
from botocore.exceptions import ClientError
from boto3.dynamodb.conditions import Key, Attr
import pandas as pd
from datetime import datetime
from pytz import timezone
from pycollector.video import Video
from copy import deepcopy

import pycollector
from pycollector.admin.workforce import Collectors
from pycollector.admin.dropbox import Dropbox
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Set to target ENV
backend = pycollector.admin.globals.backend(env="prod", org="visym")

s3_client = boto3.client("s3")
s3_resource = boto3.resource("s3")
dynamoDB_resource = boto3.resource("dynamodb")

video_filetype = ".mp4"
public_bucketname = backend.s3_public_bucket()


def time_format(dt):
    return "%s:%.6f%s" % (
        dt.strftime("%Y-%m-%dT%H:%M"),
        float("%.6f" % (dt.second + dt.microsecond / 1e6)),
        dt.strftime("%z"),
    )


def getUpdatedThumbnails(input_video_path, input_annotation_path):

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

            thumbnail_file_stats = os.stat(thumbnail_file_path)
            print(f"File Size in MegaBytes is {thumbnail_file_stats.st_size / (1024 * 1024)}")

        except Exception as e:
            logger.warning(
                "Not able to generate thumbnail for video file {0} and JSON file {1} due to exception: {2} ".format(
                    input_video_path, input_annotation_path, e
                )
            )
            thumbnail_file_path = "None"
            thumbnail_small_file_path = "None"

    else:
        # Generateing thumbnail from activities
        for idx, a in enumerate(v.activityclip()):
            if idx == 0:
                try:
                    # a.clone().centersquare().maxdim(512).centercrop( (256, 512) ).thumbnail(thumbnail_file_path, frame=0, dpi=200)
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
                    thumbnail_small_file_path = "None"

    return thumbnail_file_path, thumbnail_small_file_path


def handler(event, context):
    for record in event["Records"]:
        bucket = record["s3"]["bucket"]["name"]
        key = unquote_plus(record["s3"]["object"]["key"])
        this_filename = key.split("/")[-1]
        saved_edited_json_path = "/tmp/{}".format(this_filename)
        logger.info("With event: {0}".format(event))

        # Check if the key contain "_" to indicate this json file is coming from an edited video
        if "_" in this_filename:
            logger.info("Received edited JSON file {0}".format(this_filename))
            # move rename the
            og_filename = "".join([this_filename.split("_")[0], ".json"])
            saved_original_json_path = "/tmp/{}".format(og_filename)

            original_json_key = "/".join(key.split("/")[:-1]) + "/" + og_filename
            original_video_key = original_json_key.replace(".json", ".mp4")
            archive_s3_path = "edited_videos_archives/" + "/".join(key.split("/")[1:-1]) + "/" + this_filename

            #############################################################
            # Download both edited JSON and original JSON to /tmp
            #############################################################

            # Download edited JSON to saved_edited_json_path
            try:
                s3_client.download_file(bucket, key, saved_edited_json_path)
            except Exception as e:
                logger.info("Download edited JSON Exception: {0}".format(e))

            # Download original video to saved_original_video_path
            try:
                s3_client.download_file(bucket, original_video_key, saved_original_json_path.replace(".json", ".mp4"))
            except Exception as e:
                logger.info("Download original video Exception: {0}".format(e))

            # get the size of the video file
            size = os.path.getsize(saved_original_json_path.replace(".json", ".mp4"))
            logger.info("Downloaded original video Size is {0} bytes".format(size))

            #############################################################
            # Update video thumbnails in DDB
            #############################################################
            thumbnail_file_path, thumbnail_small_file_path = getUpdatedThumbnails(
                saved_original_json_path.replace(".json", ".mp4"), saved_edited_json_path
            )

            # # get the size of the thumbnail_small file
            # size = os.path.getsize(thumbnail_small_file_path)
            # print('thumbnail_small_file Size is', size, 'bytes')

            print("thumbnail_file_path", thumbnail_file_path)
            print("thumbnail_small_file_path", thumbnail_small_file_path)

            public_s3_url_template = "https://{0}.s3.amazonaws.com/{1}"

            # Upload thumbnails to S3 and return S3 thumbnail urls
            program_name = key.split("/")[2]
            project_id = key.split("/")[3]
            video_id = og_filename.replace(".json", "")

            thumbnail_filename = (
                program_name + "_" + project_id.replace(" ", "__") + "_" + video_id + "_{:%Y%m%d_%H%M%S}".format(datetime.now()) + "_thumbnail.jpg"
            )
            thumbnail_small_filename = (
                program_name
                + "_"
                + project_id.replace(" ", "__")
                + "_"
                + video_id
                + "_{:%Y%m%d_%H%M%S}".format(datetime.now())
                + "_thumbnail_small.jpg"
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

            #############################################################
            # Update video thumbnails in DDB
            ##############################################################

            videos_table = backend.ddb_video()

            # query data
            try:
                video_response = videos_table.query(IndexName=("video_id-index"), KeyConditionExpression=Key("video_id").eq(video_id))
            except ClientError as e:
                logger.warning("Not able to query video table in DDB due to Exception: ")
                logger.warning(e.response["Error"]["Message"])

            #############################################################
            # Update dropbox link for the new edited annotation
            #############################################################

            # Store videos and json to dropbox
            t = Collectors().dropbox_token(email=video_response["Items"][0]["collector_email"])

            if t is not None:
                d = Dropbox(token=t)
                try:
                    d.upload(saved_edited_json_path)
                    video_response["Items"][0]["json_sharing_link"] = d.link(saved_edited_json_path)
                except Exception as e:
                    logger.info("Not able to upload annotation file to dropbox due to: {0}".format(e))

            # construct the new data record
            old_video_response = deepcopy(video_response)
            video_uploaded_datetime = datetime.now(timezone("US/Eastern")).isoformat()
            video_response["Items"][0]["uploaded_date"] = video_uploaded_datetime
            # datetime.strptime(
            #     time_format(datetime.now(timezone("US/Eastern"))), "%Y-%m-%dT%H:%M:%S.%f%z"
            # ).isoformat()
            video_response["Items"][0]["updated_datetime"] = video_uploaded_datetime  # datetime.now(timezone("US/Eastern")).isoformat()
            video_response["Items"][0]["thumbnail"] = thumbnail_url
            video_response["Items"][0]["thumbnail_small"] = thumbnail_small_url
            video_response["Items"][0]["annotation_file_path"] = key
            video_response["Items"][0]["video_state"] = "Edited"

            # Insert new video with new uploaded_date
            try:
                put_new_video_response = videos_table.put_item(Item=video_response["Items"][0])
            except Exception as e:
                print("Can not add new item due to : ", e)

            # Delete old video with old uploaded_date
            try:
                delete_old_video_response = videos_table.delete_item(
                    Key={
                        "id": old_video_response["Items"][0]["id"],
                        "uploaded_date": old_video_response["Items"][0]["uploaded_date"],
                    }
                )
            except Exception as e:
                print("Can not delete item due to : ", e)

            # TODO - Temp to add os.environ should be taken cared. by pycollector.admin.backend
            current_credentials = boto3.Session().get_credentials().get_frozen_credentials()
            os.environ["VIPY_AWS_ACCESS_KEY_ID"] = current_credentials.access_key
            os.environ["VIPY_AWS_SECRET_ACCESS_KEY"] = current_credentials.secret_key

            # Delete old instances
            instances = pycollector.admin.video.Video(video_id).instances()
            instance_table = backend.ddb_instance()
            with instance_table.batch_writer() as batch:
                for instance in instances:
                    instance_id = instance.instanceid()
                    iid = instance._instance["id"]
                    batch.delete_item(Key={"id": iid, "instance_id": instance_id})

            # Delete old review assignments
            review_assignment_table = backend.ddb_review_assignment()

            response = review_assignment_table.query(
                IndexName="ByVideoId",
                KeyConditionExpression=Key("video_id").eq(video_id),
            )
            assignments = response["Items"]
            while "LastEvaluatedKey" in response and response["LastEvaluatedKey"] is not None:
                response = rating_table.query(
                    IndexName="ByVideoId",
                    KeyConditionExpression=Key("video_id").eq(video_id),
                    ExclusiveStartKey=response["LastEvaluatedKey"],
                )
                assignments.extend(response["Items"])

            with review_assignment_table.batch_writer() as batch:
                for assignment in assignments:
                    collector_id = assignment["collector_id"]
                    video_id = assignment["video_id"]
                    batch.delete_item(Key={"collector_id": collector_id, "video_id": video_id})

            # resubmit the video by invoking the video processing function?
            lambda_config = botocore.config.Config(read_timeout=900, connect_timeout=900, retries={"max_attempts": 0})

            lambda_client = boto3.client("lambda", config=lambda_config)

            # Invoke Lambda function
            request = {
                "Records": [
                    {
                        "is_edited": True,
                        "video_uploaded_datetime": video_uploaded_datetime,
                        "s3": {
                            "s3SchemaVersion": "1.0",
                            "bucket": {
                                "name": backend.s3_bucket(),
                                "ownerIdentity": {"principalId": "A2JRIAVS5OMKKC"},
                                "arn": "arn:aws:s3:::{0}".format(backend.s3_bucket()),
                            },
                            "object": {"key": original_json_key.replace(".json", ".mp4")},
                        },
                    }
                ]
            }

            response = lambda_client.invoke(
                # TODO - may reconstruct the arn with aws region, id and environment_name
                FunctionName=backend.video_processing_lambda_function_arn(),
                InvocationType="RequestResponse",
                LogType="Tail",
                Payload=json.dumps(request),
            )
            return {"statusCode": 200, "body": json.dumps("Edited Videos Processed successfully!")}
        else:
            # Do nothing
            logger.info("Received original JSON file {0}".format(this_filename))
            return {
                "statusCode": 200,
                "body": json.dumps("This is original JSON file. There is no need for processing."),
            }
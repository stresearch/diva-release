# from collector.review import score_verified_instance_by_id

import boto3
from botocore.exceptions import ClientError
from boto3.dynamodb.conditions import Key, Attr

import json
import time
from datetime import datetime, timedelta
from pytz import timezone, utc
import decimal
import pandas as pd

import pycollector
from pycollector.admin.backend import Backend


# Set to target ENV
backend = pycollector.admin.globals.backend(env="dev", org="visym")


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


def get_collector_email(collector_id=None):
    """Get collectors' emails"""

    kce = Key("collector_id").eq(collector_id)

    collectors_table = backend.ddb_collector()

    response = collectors_table.query(
        KeyConditionExpression=kce,
    )

    return list(pd.DataFrame(response["Items"])["collector_email"])[0]


def set_attributes(event):
    """"""

    Image_Age_str = "NewImage"

    if "NewImage" not in event["Records"][0]["dynamodb"]:
        Image_Age_str = "OldImage"

    print(" event['Records'][0]['dynamodb']: ", event["Records"][0]["dynamodb"])
    print(" event['Records'][0]['dynamodb'][Image_Age_str]: ", event["Records"][0]["dynamodb"][Image_Age_str])

    week = event["Records"][0]["dynamodb"][Image_Age_str]["week"]["S"]
    submitted_time = event["Records"][0]["dynamodb"][Image_Age_str]["submitted_time"]["S"]
    this_reviewer = event["Records"][0]["dynamodb"][Image_Age_str]["reviewer_id"]["S"]
    instance_id = event["Records"][0]["dynamodb"]["Keys"]["id"]["S"]
    video_id = event["Records"][0]["dynamodb"][Image_Age_str]["video_id"]["S"]
    video_uploaded_date = event["Records"][0]["dynamodb"][Image_Age_str]["video_uploaded_date"]["S"]

    if "S" in event["Records"][0]["dynamodb"][Image_Age_str]["rating_responses"]:
        rating_answer = [a for a in event["Records"][0]["dynamodb"][Image_Age_str]["rating_responses"]["S"].split(",")]
    elif "L" in event["Records"][0]["dynamodb"][Image_Age_str]["rating_responses"]:
        rating_answer = [v["S"] for v in event["Records"][0]["dynamodb"][Image_Age_str]["rating_responses"]["L"]]

    # OG
    # response_list = [ 'bad_box_big', 'bad_box_small', 'bad_label', 'bad_timing', 'bad_viewpoint', 'bad_alignment', 'bad_scene', 'bad_visibility', 'bad_video', 'good_for_training', 'awkward_scene', 'good']

    # New
    response_list = [
        "bad_box_big",
        "bad_box_small",
        "bad_label",
        "bad_timing",
        "bad_viewpoint",
        "bad_joint_label",
        "warning",
        "bad_style",
        "bad_location",
        "bad_illumination",
        "bad_pose",
        "bad_alignment",
        "bad_visibility",
        "bad_video",
        "good_for_training",
        "good",
    ]

    # rating table object
    Rating_table = backend.ddb_rating()

    if "@" not in this_reviewer:
        delete_rating_response = Rating_table.delete_item(
            Key={
                "id": instance_id,
                "reviewer_id": this_reviewer,
            }
        )

        this_reviewer = get_collector_email(collector_id=this_reviewer)

    responses = {}

    item = {
        "id": instance_id,
        "reviewer_id": this_reviewer,
        "video_id": video_id,
        "rating_responses": rating_answer,
        "transformed": True,
        "submitted_time": submitted_time,
        "week": week,
        "updated_week": (
            datetime.now(timezone("US/Eastern")).date() - timedelta(days=datetime.now(timezone("US/Eastern")).date().weekday())
        ).isoformat(),
        "video_uploaded_date": video_uploaded_date,
    }

    for response in response_list:
        item[response] = 0

    if isinstance(rating_answer, list):
        for answer in rating_answer:
            if answer in item:
                item[answer] = 1
    elif isinstance(rating_answer, str):
        for answer in rating_answer.split(","):
            if answer in item:
                item[answer] = 1

    excluding_attributes = [
        "transformed",
        "video_id",
        "reviewer_id",
        "id",
        "rating_responses",
        "week",
        "updated_week",
        "verified",
        "submitted_time",
        "video_uploaded_date",
    ]

    update_item = {}
    for k, v in item.items():
        if k not in excluding_attributes:
            update_item[":" + k] = decimal.Decimal(str(v))

    UpdateExpression = "set " + ", ".join(["{0} = :{1}".format(k, k) for k, v in item.items() if k not in excluding_attributes])
    UpdateExpression = (
        UpdateExpression
        + ", verified = :verified, updated_time = :updated_time, submitted_time =:submitted_time, transformed = :transformed, video_id = :video_id, week = :week, updated_week = :updated_week, rating_responses = :rating_responses, video_uploaded_date = :video_uploaded_date"
    )

    update_item[":verified"] = True
    update_item[":transformed"] = True
    update_item[":updated_time"] = datetime.now(timezone("US/Eastern")).isoformat()
    update_item[":video_id"] = item["video_id"]
    update_item[":week"] = item["week"]
    update_item[":rating_responses"] = item["rating_responses"]
    update_item[":updated_week"] = item["updated_week"]
    update_item[":video_uploaded_date"] = item["video_uploaded_date"]

    if "/" in item["submitted_time"]:
        update_item[":submitted_time"] = datetime.strptime(item["submitted_time"], "%m/%d/%Y, %I:%M:%S %p").isoformat()
    elif "-" in item["submitted_time"]:
        update_item[":submitted_time"] = datetime.strptime(item["submitted_time"], "%m/%d/%Y, %I:%M:%S %p").isoformat()

    response = Rating_table.update_item(
        Key={"id": item["id"], "reviewer_id": item["reviewer_id"]},
        UpdateExpression=UpdateExpression,
        ExpressionAttributeValues=update_item,
        ReturnValues="UPDATED_NEW",
    )


def handler(event, context):

    print("event: ", event)
    print("context: ", context)

    OldImage_dict = None
    NewImage_dict = None

    if "OldImage" in event["Records"][0]["dynamodb"]:
        OldImage_dict = event["Records"][0]["dynamodb"]["OldImage"]

    if "NewImage" in event["Records"][0]["dynamodb"]:
        NewImage_dict = event["Records"][0]["dynamodb"]["NewImage"]

    print("OldImage_dict: ", OldImage_dict)
    print("NewImage_dict: ", NewImage_dict)

    # isAttributeTransformed = is_attribute_transformed(event)

    instance_id = event["Records"][0]["dynamodb"]["Keys"]["id"]["S"]

    # if isAttributeTransformed == False:
    if event["Records"][0]["eventName"] == "INSERT":
        print("Run transformation")
        try:
            set_attributes(event)
        except Exception as e:
            print("Exception: ", e)
            # Publish_SNS(
            #     subject="AWS Notification Message - Not able to unpack rating response during insert for instance: {0} ".format(
            #         instance_id
            #     ),
            #     error_message="Not able to unpack rating response during insert for {0} due to exception: {1} ".format(
            #         instance_id, e
            #     ),
            # )
            return None
        return
    elif (event["Records"][0]["eventName"] == "MODIFY") and ("transformed" not in event["Records"][0]["dynamodb"]["NewImage"]):

        try:
            set_attributes(event)
        except Exception as e:
            print("Exception: ", e)

            # Publish_SNS(
            #     subject="AWS Notification Message - Not able to unpack rating response during modify for instance: {0} ".format(
            #         instance_id
            #     ),
            #     error_message="Not able to unpack rating response during modify for {0} due to exception: {1} ".format(
            #         instance_id, e
            #     ),
            # )

            return None
        return
    else:

        print("Scoring instance!")

        instance_id = event["Records"][0]["dynamodb"]["Keys"]["id"]["S"]  # event[["instance_id"]

        # Ensure to wait for the rating of the instance to be insert into the DynamoDB
        time.sleep(3)

        try:
            instances_rating_data = score_verified_instance_by_id(instance_id=instance_id)
        except Exception as e:
            print("exception: ", e)
            # Publish_SNS(
            #     subject="AWS Notification Message - Not able to score for instance: {0} ".format(
            #         instance_id
            #     ),
            #     error_message="Not able to score for {0} due to exception: {1} ".format(
            #         instance_id, e
            #     ),
            # )
            return None

        print("instances_rating_data: ", instances_rating_data)

        return {
            "statusCode": 200,
            "body": json.dumps(
                "Successfully updated scores for instance: {0} , and the parent video: {1} !".format(
                    instances_rating_data["id"],
                    instances_rating_data["video_id"],
                )
            ),
        }


def score_verified_instance_by_id(instance_id, uploaded_datetime_since=None, verbose=True, this_reviewer=None):
    """ Scoring the the recent verified viideos by week """

    score_names = [
        "bad_box_big_score",
        "bad_box_smal_scorel",
        "bad_label_score",
        "bad_timing_score",
        "bad_viewpoint_score",
        "bad_joint_label_score",
        "bad_style_score",
        "bad_location_score",
        "bad_illumination_score",
        "bad_pose_score",
        "bad_alignment_score",
        "bad_visibility_score",
        "bad_video_score",
        "good_for_training_score",
        "awkward_scene_score",
        "rating_score",
    ]

    if verbose:
        print(" Scoring the the recent verified instances and respected video for instance id {} ...".format(instance_id))

    # Get video scores by video_ids
    try:
        (
            instances_rating_data,
            excluding_instance_Ids,
        ) = calculate_usefulness_scores_by_instance_id(instance_id, this_reviewer=this_reviewer)
    except Exception as exception:
        print(
            " Not able to score the the recent verified instances and respected video for instance id {0}, with exception: {1}".format(
                instance_id, exception
            )
        )

    # print("instances_rating_data: ", instances_rating_data)

    # Update Instances by instance id
    instance_PK = get_partition_key_by_instance_id(instance_id)
    instances_rating_data["Instance_PK"] = instance_PK

    score_instance_by_data(instances_rating_data)

    # Update associated video
    # Get all sibling instances by Video_ID

    all_related_instances_df = get_activity_instances(
        video_id=instances_rating_data["video_id"],
    ).fillna(0)

    # Fill 0 for new attribute in old records
    for name in score_names:
        if name not in all_related_instances_df:
            all_related_instances_df[name] = 0

    all_related_instances_df[score_names] = all_related_instances_df[score_names].apply(pd.to_numeric)

    group_by_video_ids_mean_df = all_related_instances_df.groupby("video_id")[score_names].mean()

    group_by_video_ids_mean_df["video_id"] = group_by_video_ids_mean_df.index
    group_by_video_ids_mean_df["project_name"] = list(all_related_instances_df.project_name)[0]
    group_by_video_ids_mean_df["collection_name"] = list(all_related_instances_df.collection_name)[0]
    group_by_video_ids_mean_df["collector_id"] = list(all_related_instances_df.collector_id)[0]

    # Get Video uploaded date

    sort_key = get_sort_key_by_video_id(instances_rating_data["video_id"])

    group_by_video_ids_mean_df["sort_key"] = sort_key

    for idx, row in group_by_video_ids_mean_df.iterrows():

        score_video_by_data(row)

    return instances_rating_data


def calculate_usefulness_scores_by_instance_id(instance_id, this_reviewer=None):
    """ Calculate usefulness scores by given instance id """

    # print("calculating usefulness scores by given instance id")

    response_list = [
        "bad_box_big",
        "bad_box_small",
        "bad_label",
        "bad_timing",
        "bad_viewpoint",
        "bad_joint_label",
        "warning",
        "bad_style",
        "bad_location",
        "bad_illumination",
        "bad_pose",
        "bad_alignment",
        "bad_visibility",
        "bad_video",
        "good_for_training",
        "good",
    ]

    # rating table object
    Rating_table = backend.ddb_rating()

    Rated_instances_response = Rating_table.query(
        KeyConditionExpression=Key("id").eq(instance_id),
    )

    Rated_instances_df = pd.DataFrame(Rated_instances_response["Items"]).fillna(0)

    # Filter with latest video_uploaded_date to ensure we only get the latest results
    latest_video_uploaded_date = Rated_instances_df["video_uploaded_date"].max()
    Rated_instances_df = Rated_instances_df[Rated_instances_df["video_uploaded_date"] == latest_video_uploaded_date]

    total_rated_counts = len(Rated_instances_df)
    video_id = list(Rated_instances_df.video_id)[0]
    reviewer_id = list(Rated_instances_df.reviewer_id)[0]
    instance_id = list(Rated_instances_df.id)[0]

    responses_dfs = []
    for responose in response_list:

        # TODO - tmp solution until we have an unify rating score
        response_name = responose

        if responose == "good":
            response_name = "rating"
        try:
            responses_dfs.append(
                pd.Series(
                    Rated_instances_df.groupby(["id"])[responose].sum() / total_rated_counts,
                    name=response_name + "_score",
                )
            )
        except Exception as e:
            responses_dfs.append(
                pd.Series(
                    decimal.Decimal(0),
                    index=[instance_id],
                    name=response_name + "_score",
                )
            )
            continue

    instance_rating_df = pd.concat(responses_dfs, axis=1, sort=False).fillna(0)

    instance_rating_df["id"] = instance_rating_df.index
    instance_rating_df["video_id"] = video_id
    instance_rating_df["reviewer_id"] = reviewer_id

    excluding_instance_ids = list(Rated_instances_df[Rated_instances_df.reviewer_id == this_reviewer].id)

    return instance_rating_df.iloc[0].to_dict(), excluding_instance_ids


def get_partition_key_by_instance_id(instance_id):
    """ Get partition key by instance_id """

    # rating table object
    instances_table = backend.ddb_instance()

    response = instances_table.query(
        IndexName="instance_id-index",
        KeyConditionExpression=Key("instance_id").eq(instance_id),
    )
    # print(pd.DataFrame(response["Items"]))
    # print(instance_id)
    return list(pd.DataFrame(response["Items"])["id"])[0]


def score_instance_by_data(data):
    """update instance table with usefulness data """

    excluding_attributes = ["Instance_PK", "video_id", "reviewer_id", "id"]

    update_item = {}
    for k, v in data.items():
        if k not in excluding_attributes:
            update_item[":" + k] = v

    UpdateExpression = "set " + ", ".join(["{0} = :{1}".format(k, k) for k, v in data.items() if k not in excluding_attributes])
    UpdateExpression = UpdateExpression + ", verified = :verified, updated_date = :updated_date"

    update_item[":verified"] = True
    update_item[":updated_date"] = datetime.now(timezone("US/Eastern")).isoformat()

    # rating table object
    instances_table = backend.ddb_instance()

    instances_table_update_response = instances_table.update_item(
        Key={"id": data["Instance_PK"], "instance_id": data["id"]},
        UpdateExpression=UpdateExpression,
        ExpressionAttributeValues=update_item,
        ReturnValues="UPDATED_NEW",
    )


def get_sort_key_by_video_id(video_id):
    """ Get sort key by video_id """

    # rating table object
    videos_table = backend.ddb_video()

    response = videos_table.query(
        IndexName="video_id-index",
        KeyConditionExpression=Key("video_id").eq(video_id),
    )

    return list(pd.DataFrame(response["Items"])["uploaded_date"])[0]


def get_activity_instances(
    activity_name=None,
    project_id=None,
    collection_id=None,
    instance_ids=None,
    week=None,
    startdate=None,
    enddate=None,
    verified=None,
    collector_id=None,
    video_id=None,
    video_ids=None,
    project_ids=None,
    collection_ids=None,
    ex_project_ids=None,
    ex_collection_ids=None,
    return_self=False,
):
    """[summary]

    Keyword Arguments:
        activity_name {[type]} -- [description] (default: {None})
        project_id {[type]} -- [description] (default: {None})
        collection_id {[type]} -- [description] (default: {None})
        startdate {[type]} -- [description] (default: {None})
        enddate {[type]} -- [description] (default: {None})
        verified {bool} -- [description] (default: {False})
        collector_id {[type]} -- [description] (default: {None})
        video_id {[type]} -- [description] (default: {None})
        topN {int} -- [description] (default: {10000})
        return_self {bool} -- [description] (default: {False})

    Returns:
        [type] -- [description]
    """

    # Check for all filtering conditions
    conditions = locals()
    conditions = {k: v for k, v in conditions.items() if v != None}
    conditions = {k: v for k, v in conditions.items() if k not in ["self", "topN", "return_self"]}

    FilterExpressions = []
    for k, v in conditions.items():
        if k == "startdate":
            FilterExpressions.append(" uploaded_date >= :{0}".format(k))
        elif k == "enddate":
            FilterExpressions.append(" uploaded_date <= :{0}".format(k))
        elif k == "video_ids":
            FilterExpressions.append(" video_id IN  :{0}".format(k))
        elif k == "week" and week:
            continue
        else:
            FilterExpressions.append(" {0} = :{0}".format(k))

    FilterExpression = " and ".join(FilterExpressions)

    ExpressionAttributeValues = {}

    for k, v in conditions.items():
        if k == "week" and week:
            continue

        ExpressionAttributeValues[":{}".format(k)] = v

    # Set up the filter expression
    if startdate and enddate:
        startdate = datetime.datetime.strptime(startdate, "%Y-%m-%d").isoformat()
        enddate = datetime.datetime.strptime(enddate, "%Y-%m-%d").isoformat()
    elif not startdate:
        startdate = week

    # rating table object
    instances_table = backend.ddb_instance()

    # TODO - Temp workaround for using list
    if video_ids:
        FilterExpression = Attr("video_id").is_in(video_ids)
        ExpressionAttributeValues = {}

    elif instance_ids:
        FilterExpression = Attr("instance_id").is_in(instance_ids)
        ExpressionAttributeValues = {}

    items = None

    # Use Query operation if we have all the necessary elements for the partition key
    if activity_name and project_id and collection_id:
        kce = Key("id").eq(partition_key)

        response = instances_table.query(
            KeyConditionExpression=kce,
            FilterExpression=FilterExpression,
            ExpressionAttributeValues=ExpressionAttributeValues,
        )
    elif video_id:

        response = instances_table.query(
            IndexName="ByStrVideoId",
            KeyConditionExpression=Key("video_id").eq(video_id),
            ScanIndexForward=False,
        )
        items = response["Items"]
    elif week:

        if verified is not None:
            response = instances_table.query(
                IndexName="week-uploaded_date-index",
                KeyConditionExpression=Key("week").eq(week),
                FilterExpression=FilterExpression,
                ExpressionAttributeValues=ExpressionAttributeValues,
            )

            items = response["Items"]
            while "LastEvaluatedKey" in response and response["LastEvaluatedKey"] is not None:
                response = instances_table.query(
                    IndexName="week-uploaded_date-index",
                    KeyConditionExpression=Key("week").eq(week),
                    FilterExpression=FilterExpression,
                    ExpressionAttributeValues=ExpressionAttributeValues,
                    ExclusiveStartKey=response["LastEvaluatedKey"],
                )

                items.extend(response["Items"])

        else:
            response = instances_table.query(
                IndexName="week-uploaded_date-index",
                KeyConditionExpression=Key("week").eq(week),
            )

            items = response["Items"]
            while "LastEvaluatedKey" in response and response["LastEvaluatedKey"] is not None:
                response = instances_table.query(
                    IndexName="week-uploaded_date-index",
                    KeyConditionExpression=Key("week").eq(week),
                    ExclusiveStartKey=response["LastEvaluatedKey"],
                )

                items.extend(response["Items"])

    # Use Scan operation otherwise
    else:
        response = instances_table.scan(
            FilterExpression=FilterExpression,
            ExpressionAttributeValues=ExpressionAttributeValues,
        )

    if items == []:
        return None

    results_df = pd.DataFrame(items).sort_values(["uploaded_date"], ascending=False)

    # Filteres
    if project_ids:
        results_df = results_df[results_df["project_id"].isin(project_ids)]
    if collection_ids:
        results_df = results_df[results_df["collection_id"].isin(collection_ids)]

    if ex_collection_ids:
        results_df = results_df[~results_df["collection_id"].isin(ex_collection_ids)]

    return results_df


def get_partition_id(combination_liist):
    return "_".join(combination_liist)


def score_video_by_data(data):
    """update video table with usefulness data """

    excluding_attributes = [
        "Instance_PK",
        "video_id",
        "collector_id",
        "id",
        "project_name",
        "collection_name",
        "sort_key",
    ]

    update_item = {}
    for k, v in data.items():
        if k not in excluding_attributes:
            update_item[":" + k] = decimal.Decimal(str(v))

    UpdateExpression = "set " + ", ".join(["{0} = :{1}".format(k, k) for k, v in data.items() if k not in excluding_attributes])
    UpdateExpression = UpdateExpression + ", verified = :verified, updated_date = :updated_date"

    update_item[":verified"] = True
    update_item[":updated_date"] = datetime.now(timezone("US/Eastern")).isoformat()

    # rating table object
    videos_table = backend.ddb_video()

    videos_table_update_response = videos_table.update_item(
        Key={
            "id": data.collection_name + "_" + data.project_name + "_" + data.collector_id,
            "uploaded_date": data.sort_key,
        },
        UpdateExpression=UpdateExpression,
        ExpressionAttributeValues=update_item,
        ReturnValues="UPDATED_NEW",
    )
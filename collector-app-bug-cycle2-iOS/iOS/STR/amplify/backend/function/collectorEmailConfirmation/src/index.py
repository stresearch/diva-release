import json
import boto3
from boto3.dynamodb.conditions import Key, Attr
import botocore.exceptions
import hmac
import hashlib
import base64
import uuid
import pycollector
from pycollector.admin.backend import Backend
from datetime import datetime


# Set to target ENV
backend = pycollector.admin.globals.backend(env="prod", org="visym")


def handler(event, context):

    print("event: ", event)

    email = event["request"]["userAttributes"]["email"]
    userId = event["request"]["userAttributes"]["sub"]
    user_status = event["request"]["userAttributes"]["cognito:user_status"]
    firstname = event["request"]["userAttributes"]["custom:first_name"]
    lastname = event["request"]["userAttributes"]["custom:last_name"]
    query_attribute = "1"

    # Create user data in DynamoDB
    client = boto3.resource("dynamodb")

    co_Collectors_table = backend.ddb_collector()

    # For first time user registeraton - Check if user already exisit
    collector_response = co_Collectors_table.query(KeyConditionExpression=Key("collector_id").eq(userId))

    if collector_response["Items"] == []:

        dynamoDB_create_user_response = co_Collectors_table.put_item(
            Item={
                "collector_id": userId,
                "collector_email": email,
                "first_name": firstname,
                "last_name": lastname,
                "query_attribute": query_attribute,
                "updatedAt": str(datetime.now().date()),
                "uploaded_count": 0,
            }
        )

        # Assign Default Collections to new collector
        B = pycollector.admin.globals.backend("prod")
        C = B.collections(program="Practice").filter(project=["Getting Started", "Tutorial"]).collectionlist()
        B.collection_assignment().assign(userId, collectionlist=C).sync()

    return event

import json
import boto3
from botocore.exceptions import ClientError
from boto3.dynamodb.conditions import Key, Attr
import pycollector

# Set to target ENV
backend = pycollector.admin.globals.backend(env="dev", org="visym")

_CLIENT_ID = backend.cognito_client_id()


def handler(event, context):

    print("event: ", event)
    print("Username: ", event["request"]["userAttributes"]["sub"])

    # Check if user is already in the user group
    cognito_idp_client = boto3.client("cognito-idp")
    user_pool_id = event["userPoolId"]
    user_email = event["request"]["userAttributes"]["email"]
    clientId = event["callerContext"]["clientId"]

    response = cognito_idp_client.admin_list_groups_for_user(
        Username=event["request"]["userAttributes"]["sub"],
        UserPoolId=user_pool_id,
        Limit=20,
        # NextToken='string'
    )
    print("response: ", response)

    # User group:
    if (response["Groups"] == []) and (clientId == _CLIENT_ID):
        print("Add user to the group")
        add_group_response = cognito_idp_client.admin_add_user_to_group(
            UserPoolId=user_pool_id, Username=user_email, GroupName="collector_Python_API"
        )
        return event
    else:
        print("We have the user in group")
        return event

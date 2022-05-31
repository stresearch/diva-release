//
//  ProjectsAPI.swift
//  STR
//
//  Created by Srujan on 09/04/20.
//  
//

import Foundation
import AWSAppSync
import AWSMobileClient

enum ProjectsAPI {
    
    static func getCollectorCollections(token: String, completionHandler: @escaping
    ([ListStrCollectionsAssignmentsQuery.Data.ListStrCollectionsAssignment.Item]?, ApiError?, String?) -> Void) {
        
        let filter = ModelstrCollectionsAssignmentFilterInput(active: ModelBooleanInput(eq: true))
        
        AppDelegate.appSyncClient?.fetch(query: ListStrCollectionsAssignmentsQuery(collector_id: Collector.currentCollector.userId, filter: filter, limit: 50, nextToken: token == "" ? nil : token), cachePolicy: .fetchIgnoringCacheData) { (result, error) in
            var err = ApiError(title: nil, message: "", code: nil)
            if let error = error as? AWSAppSyncClientError {
                err.message = error.errorDescription
                switch error.errorCode {
                case ApiErrorCode.noInternet.rawValue:
                    err.message = LocalizableString.checkInternet.localizedString
                    err.code = .noInternet
                    completionHandler(nil, err, nil)
                default:
                    completionHandler(nil,err, nil)
                }
            } else if let resultError = result?.errors {
                err.message = resultError.description
                completionHandler(nil,err, nil)
            } else {
                completionHandler(
                    (result?.data?.listStrCollectionsAssignments?.items)! as? [ListStrCollectionsAssignmentsQuery.Data.ListStrCollectionsAssignment.Item],
                    nil, result?.data?.listStrCollectionsAssignments?.nextToken
                )
            }
        }
        
    }
}

extension AWSAppSyncClientError {
    
    public var errorReason: String? {
        let underlyingError: Error?
        var message: String
        let errorResponse: HTTPURLResponse?

        switch self {
        case .requestFailed(_, let response, let error):
            errorResponse = response
            underlyingError = error
            message = "Did not receive a successful HTTP code."
        case .noData(let response):
            errorResponse = response
            underlyingError = nil
            message = "No Data received in response."
        case .parseError(_, let response, let error):
            underlyingError = error
            errorResponse = response
            message = "Could not parse response data."
        case .authenticationError(let error):
            underlyingError = error
            errorResponse = nil
            message = "Failed to authenticate request."
        }

        if let error = underlyingError {
            message += "\(error.localizedDescription)"
        }

        if let _ = errorResponse {
            return "\(message)"
        } else {
            return "\(message)"
        }
    }
    
    public var errorCode: Int? {
        switch self {
        case .requestFailed(_, _, let error):
            return (error! as NSError).code
        case .noData(_):
            return nil
        case .parseError(_, _, let error):
            return (error! as NSError).code
        case .authenticationError(let error):
            return (error as NSError).code
        }
    }
    
}

//
//  VideoAPI.swift
//  STR
//
//  Created by Srujan on 14/04/20.
//  
//

import Foundation
import AWSAppSync

enum VideoAPI {
    
    static func updateVideo(videoDevInput: UpdateStrVideosInput, completionHandler: @escaping (Bool, ApiError?) -> Void) {
        
        AppDelegate.appSyncClient?.perform(mutation: UpdateStrVideosMutation(input: videoDevInput)) { (result, error) in
            
            var err = ApiError(title: nil, message: "", code: nil)
            
            if let error = error as? AWSAppSyncClientError {
                err.message = error.errorDescription
                switch error.errorCode {
                case ApiErrorCode.noInternet.rawValue:
                    err.message = LocalizableString.checkInternet.localizedString
                    err.code = .noInternet
                    completionHandler(false, err)
                default:
                    completionHandler(false,err)
                }
            } else if let resultError = result?.errors {
                err.message = resultError.description
                completionHandler(false, err)
            } else {
                completionHandler(true, nil)
            }
        }
    }
    
    static func getCollectionVideos(token: String, completionHandler: @escaping
            ([StrVideoByUploaedDateQQuery.Data.StrVideoByUploaedDateQ.Item]?, ApiError?, String?) -> Void) {
        let filter = ModelstrVideosFilterInput(queryAttribute: ModelStringInput(eq: "1"))
        AppDelegate.appSyncClient?.fetch(query: StrVideoByUploaedDateQQuery(collector_id: Collector.currentCollector.userId, sortDirection: .desc, filter: filter, limit: 50, nextToken: token == "" ? nil : token), cachePolicy: .fetchIgnoringCacheData) { (result, error) in
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
                } else if let videos = result?.data?.strVideoByUploaedDateQ {
                    
                    completionHandler(videos.items as? [StrVideoByUploaedDateQQuery.Data.StrVideoByUploaedDateQ.Item],
                    nil, videos.nextToken)
                } else {
                    completionHandler(nil, .unboxerError, nil)
                }
        }
    }
    
    static func getEditVideoList(
        collectionId: String,
        completionHandler: @escaping (StrCollectionsByCollectionIdQuery.Data.StrCollectionsByCollectionId.Item?, ApiError?) -> Void
    ) {
        
        AppDelegate.appSyncClient?.fetch(query: StrCollectionsByCollectionIdQuery(collection_id: collectionId)) { (result, error) in
            var err = ApiError(title: nil, message: "", code: nil)
            if let error = error as? AWSAppSyncClientError {
                err.message = error.errorDescription
                switch error.errorCode {
                case ApiErrorCode.noInternet.rawValue:
                    err.message = LocalizableString.checkInternet.localizedString
                    err.code = .noInternet
                    completionHandler(nil, err)
                default:
                    completionHandler(nil,err)
                }
            } else if let resultError = result?.errors {
                err.message = resultError.description
                completionHandler(nil,err)
            } else if let result = result?.data?.strCollectionsByCollectionId {
                if result.items?.count ?? 0 > 0 {
                    completionHandler(result.items![0], nil)
                } else {
                    completionHandler(nil, nil)
                }
            } else {
                completionHandler(nil, .unboxerError)
            }
        }
    }
}

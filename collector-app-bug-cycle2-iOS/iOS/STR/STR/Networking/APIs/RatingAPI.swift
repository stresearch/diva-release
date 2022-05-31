//
//  RatingAPI.swift
//  STR
//
//  Created by SRUJAN KUMAR VEERANTI on 02/06/20.
//  
//

import Foundation
import AWSAppSync

enum RatingAPI {
    
    static func getRatingVideos(token: String, completionHandler: @escaping
    ([StrRatingVideoSortByAssignedDateQuery.Data.StrRatingVideoSortByAssignedDate.Item]?, ApiError?, String?) -> Void) {
        
        AppDelegate.appSyncClient?.fetch(query: StrRatingVideoSortByAssignedDateQuery(collector_id: Collector.currentCollector.userId, sortDirection: .desc, limit: 50, nextToken: token == "" ? nil : token), cachePolicy: .fetchIgnoringCacheData) { (result, error) in
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
            } else if let result = result?.data?.strRatingVideoSortByAssignedDate {
                if result.items?.count ?? 0 > 0 {
                    completionHandler(
                        (result.items)! as? [StrRatingVideoSortByAssignedDateQuery.Data.StrRatingVideoSortByAssignedDate.Item],
                        nil, result.nextToken
                    )
                } else {
                    completionHandler(nil, nil, nil)
                }
                
            } else {
                completionHandler(nil, .unboxerError, nil)
            }
        }
    }
    
    static func createResponseForRatingQuestions(ratingInput: CreateStrRatingInput, completionHandler: @escaping (Bool, ApiError?) -> Void) {
        
        AppDelegate.appSyncClient?.perform(mutation: CreateStrRatingMutation(input: ratingInput)) { (result, error) in
            
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
    
    static func updateResponseForRatingQuestions(ratingInput: UpdateStrRatingInput, completionHandler: @escaping (Bool, ApiError?) -> Void) {
        
        AppDelegate.appSyncClient?.perform(mutation: UpdateStrRatingMutation(input: ratingInput)) { (result, error) in
            
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
    
    static func deleteRatingVideo(videoId: String, completionHandler: @escaping (Bool, ApiError?) -> Void) {
        
        AppDelegate.appSyncClient?.perform(mutation: DeleteStrReviewAssignmentMutation(input: DeleteStrReviewAssignmentInput(collectorId: Collector.currentCollector.userId, videoId: videoId))) { (result, error) in
            
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
    
    static func getRatingResponseList(videoId: String, completionHandler: @escaping
    ([RatingByStrCollectorIdQuery.Data.RatingByStrCollectorId.Item]?, ApiError?) -> Void) {
        
        AppDelegate.appSyncClient?.fetch(query: RatingByStrCollectorIdQuery(reviewer_id: Collector.currentCollector.email, video_id: ModelStringKeyConditionInput(eq: videoId))) { (result, error) in
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
            } else if let result = result?.data?.ratingByStrCollectorId {
                if result.items?.count ?? 0 > 0 {
                    completionHandler(
                        (result.items)! as? [RatingByStrCollectorIdQuery.Data.RatingByStrCollectorId.Item],
                        nil
                    )
                } else {
                    completionHandler(nil, nil)
                }
                
            } else {
                completionHandler(nil, .unboxerError)
            }
        }
        
    }
}

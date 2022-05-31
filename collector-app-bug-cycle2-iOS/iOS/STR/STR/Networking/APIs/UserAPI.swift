//
//  UserAPI.swift
//  STR
//
//  Created by Srujan on 19/07/19.
//  
//

import Foundation
import AWSMobileClient
import AWSAppSync

enum UserAPI {

    static func getProfile(completionHandler: @escaping (Bool, ApiError?) -> Void) {
        
        AppDelegate.appSyncClient?.fetch(query: ListStrCollectorsQuery(collector_id: AWSMobileClient.default().username ?? ""), cachePolicy: .fetchIgnoringCacheData) { (result, error) in
            var err = ApiError(title: nil, message: "", code: nil)
            if let error = error as? AWSAppSyncClientError {
                err.message = error.errorDescription
                switch error.errorCode {
                case ApiErrorCode.noInternet.rawValue:
                    err.message = LocalizableString.checkInternet.localizedString
                    err.code = .noInternet
                    completionHandler(false, err)
                case ApiErrorCode.userDoesNotExist.rawValue:
                    err.message = LocalizableString.userDoesNotExist.localizedString
                    err.code = .userDoesNotExist
                    completionHandler(false, err)
                case ApiErrorCode.invalidToken.rawValue:
                    err.message = LocalizableString.invalidToken.localizedString
                    err.code = .invalidToken
                    completionHandler(false, err)
                default:
                    completionHandler(false,err)
                }
            } else if let resultError = result?.errors {
                err.message = resultError.description
                completionHandler(false,err)
            } else {
                if let result = result?.data?.listStrCollectors?.items, result.count > 0 {
                    let user = Collector.currentCollector
                    user.setCollectorDetails(item: result[0]!)
                    completionHandler(true, nil)
                } else {
                    completionHandler(false, .unboxerError)
                }
                
            }
        }
    }
    
    static func verifyCollectorEmail(collectorEmail: String, completionHandler: @escaping (StrCollectorByEmailQuery.Data.StrCollectorByEmail.Item?, ApiError?) -> Void) {
        
        AppDelegate.appSyncClient?.fetch(query: StrCollectorByEmailQuery(collector_email: collectorEmail), cachePolicy: .fetchIgnoringCacheData) { (result, error) in
            
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
                completionHandler(nil, err)
            } else if let result = result?.data?.strCollectorByEmail {
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

    
    static func getUserInfo(completionHandler: @escaping (Bool, ApiError?) -> Void) {
                
        AWSMobileClient.default().getUserAttributes { (result, error) in
            
            DispatchQueue.main.async {
                var err = ApiError(title: nil, message: "", code: nil)
                
                if let result = result {
                    let user = Collector.currentCollector
                    user.setUserDetails(dict: result)
                    completionHandler(true, nil)
                } else if let error = error as? AWSMobileClientError {
                    err.message = error.localizedDescription
                    completionHandler(false, err)
                } else if let error = error {
                    err.message = error.localizedDescription
                    
                    switch (error as NSError).code {
                    case ApiErrorCode.noInternet.rawValue:
                        err.code = .noInternet
                        completionHandler(false, err)
                    default:
                        completionHandler(false, .unboxerError)
                    }
                }
            }
        }
    }
    
    static func updateProfile(params: UserSignUpData, completionHandler: @escaping (Bool, ApiError?) -> Void) {
        
        let attributes: JSONDictionary = ["custom:first_name": params.firstName, "custom:last_name": params.lastName]
        
        AWSMobileClient.default().updateUserAttributes(attributeMap: attributes as! [String : String]) { (result, error) in
            DispatchQueue.main.async {
                var err = ApiError(title: nil, message: "", code: nil)
                
                if let _ = result {
                    
                    UserAPI.updateProfileToDDB(params: params) { (status, error) in
                        if status {
                            completionHandler(true, nil)
                        } else if let err1 = error {
                            err.message = err1.localizedDescription
                            completionHandler(false, err)
                        }
                    }
                    
                } else if let error = error as? AWSMobileClientError {
                    err.message = error.localizedDescription
                    completionHandler(false, err)
                } else if let error = error {
                    err.message = error.localizedDescription
                    
                    switch (error as NSError).code {
                    case ApiErrorCode.noInternet.rawValue:
                        err.code = .noInternet
                        completionHandler(false, err)
                    default:
                        completionHandler(false, .unboxerError)
                    }
                }
            }
        }
    }
    
    static func updateProfileToDDB(params: UserSignUpData, completionHandler: @escaping (Bool, ApiError?) -> Void) {
        
        let collector = Collector.currentCollector
        let collectorInput = UpdateStrCollectorInput(
            collectorId: collector.userId,
            collectorEmail: collector.email,
            firstName: params.firstName,
            lastName: params.lastName,
            isConsented: collector.consentSetUp,
            isDropboxIntegrated: collector.dropBoxSetUp,
            isPaypalIntegrated: collector.payPalSetUp,
            dropboxToken: collector.dropBoxToken,
            paypalEmailId: collector.payPalID
        )
        AppDelegate.appSyncClient?.perform(mutation: UpdateStrCollectorMutation(input: collectorInput)) { (result, error) in
            
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
                Collector.currentCollector.firstName = params.firstName
                Collector.currentCollector.lastName = params.lastName
                completionHandler(true, nil)
            }
        }
    }

    static func logout(completion: @escaping (Bool, ApiError?) -> Void) {

        AWSMobileClient.default().signOut { (error) in
            SplashViewController.clearDefaultsValue()
            DispatchQueue.main.async {
                var err = ApiError(title: nil, message: "", code: nil)
                if let error = error as? AWSMobileClientError {
                    err.message = error.localizedDescription
                    completion(false, err)
                } else if let error = error {
                    err.message = error.localizedDescription
                    
                    switch (error as NSError).code {
                    case ApiErrorCode.noInternet.rawValue:
                        err.code = .noInternet
                        completion(false, err)
                    default:
                        completion(false, .unboxerError)
                    }
                } else {
                    completion(true, nil)
                }
            }
        }
    }
    
    
}

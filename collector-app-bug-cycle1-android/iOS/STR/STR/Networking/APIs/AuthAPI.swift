//
//  AuthAPI.swift
//  STR
//
//  Created by Srujan on 19/07/19.
//  
//

import Foundation
import AWSMobileClient
import AWSAppSync

enum AuthAPI {
    
    static func registeruser(userData: UserSignUpData, completion: @escaping statusErrorHandler) {
        
        let params: JSONDictionary = ["custom:first_name": userData.firstName, "custom:last_name": userData.lastName]
        
        AWSMobileClient.default().signUp(username: userData.email, password: userData.password, userAttributes: params as! [String: String]) { (result, error) in
            
            DispatchQueue.main.sync {
                
                var err = ApiError(title: nil, message: "", code: nil)
                
                if let _ = result {
                    completion(true, nil)
                } else if let error = error as? AWSMobileClientError {
                    print(error)
                    switch error {
                    case .usernameExists(let message):
                        err.message = message
                        completion(false, err)
                    default:
                        completion(false, .unboxerError)
                    }
                } else if let error = error {
                    err.message = error.localizedDescription
                    
                    switch (error as NSError).code {
                    case ApiErrorCode.noInternet.rawValue:
                        err.code = .noInternet
                        completion(false, err)
                    default:
                        completion(false, .unboxerError)
                    }
                }
            }
        }
    }
    
    static func loginUser(email: String, password: String, completion: @escaping statusErrorHandler) {
        
        AWSMobileClient.default().signIn(username: email.removingWhitespaces(), password: password) { (result, error) in
        
            DispatchQueue.main.async {
            
                if let _ = result {
                    completion(true, nil)
                } else if let error = error {
                    var err = ApiError(title: nil, message: "", code: nil)
                    if let error = error as? AWSMobileClientError {
                        
                        switch(error) {
                        case .userNotConfirmed(let message):
                            err.message = message
                            completion(false, err)
                        case .userNotFound(let message):
                            err.message = message
                            completion(false, err)
                        case .invalidPassword(let message):
                            err.message = message
                            completion(false, err)
                        case .passwordResetRequired(let message):
                            err.message = message
                            completion(false, err)
                        case .aliasExists(let message):
                            err.message = message
                            completion(false, err)
                        case .notAuthorized(let message):
                            err.message = message
                            completion(false, err)
                        default:
                            completion(false, .unboxerError)
                        }
                    } else {
                        err.message = error.localizedDescription
                        
                        switch (error as NSError).code {
                        case ApiErrorCode.noInternet.rawValue:
                            err.code = .noInternet
                            completion(false, err)
                        default:
                            completion(false, .unboxerError)
                        }
                    }
                    print("\(error.localizedDescription)")
                }
            }
        }
    }

    static func forgotPassword(
        userName: String,
        completion: @escaping statusErrorHandler
    ) {
        
            AWSMobileClient.default().forgotPassword(username: userName) { (result, error) in
                DispatchQueue.main.sync {
                
                var err = ApiError(title: nil, message: "", code: nil)
                    
                if let _ = result {
                    completion(true, nil)
                } else if let error = error as? AWSMobileClientError {
                    
                    switch error {
                    case .userNotConfirmed(let message):
                        err.message = message
                        completion(false, err)
                    case .userNotFound(let message):
                        err.message = message
                        completion(false, err)
                    case .limitExceeded(let message):
                        err.message = message
                        completion(false, err)
                    default:
                        completion(false, .unboxerError)
                    }
                } else if let error = error {
                   err.message = error.localizedDescription
                   
                   switch (error as NSError).code {
                   case ApiErrorCode.noInternet.rawValue:
                       err.code = .noInternet
                       completion(false, err)
                   default:
                       completion(false, .unboxerError)
                   }
               }
            }
        }
    }
    
    static func confirmForgotPassword(
        userName: String,
        newPassword: String,
        otp: String,
        completion: @escaping statusErrorHandler
    ) {


            AWSMobileClient.default().confirmForgotPassword(username: userName, newPassword: newPassword, confirmationCode: otp) { (result, error) in
                DispatchQueue.main.sync {
                    
                var err = ApiError(title: nil, message: "", code: nil)
                
                if let _ = result {
                    completion(true, nil)
                } else if let error = error as? AWSMobileClientError {
                    
                    switch error {
                    case .codeMismatch(let message):
                        err.message = message
                        completion(false, err)
                    case .expiredCode(let message):
                        err.message = message
                    completion(false, err)
                    default:
                        completion(false, .unboxerError)
                    }
                } else if let error = error {
                  err.message = error.localizedDescription
                  
                  switch (error as NSError).code {
                  case ApiErrorCode.noInternet.rawValue:
                      err.code = .noInternet
                      completion(false, err)
                  default:
                      completion(false, .unboxerError)
                  }
              }
            }
        }
    }
    
    static func changePassword(oldPassword: String, newPassword: String, completion: @escaping statusErrorHandler) {
        
            AWSMobileClient.default().changePassword(currentPassword: oldPassword, proposedPassword: newPassword) { (error) in
                DispatchQueue.main.sync {
                    
                var err = ApiError(title: nil, message: "", code: nil)
                    
                if let error = error as? AWSMobileClientError {
                    
                    switch error {
                    case .invalidPassword(let message):
                        err.message = message
                        completion(false, err)
                    case .codeMismatch(let message):
                        err.message = message
                        completion(false, err)
                    default:
                        completion(false, .unboxerError)
                    }
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
    
    static func resendVerificationEmail(email: String, completion: @escaping statusErrorHandler) {
        
        AWSMobileClient.default().resendSignUpCode(username: email) { (result, error) in
                DispatchQueue.main.sync {
                    
                    var err = ApiError(title: nil, message: "", code: nil)
                    
                    if let _ = result {
                        completion(true, nil)
                    } else if let error = error as? AWSMobileClientError {
                        print(error)
                        switch error {
                        case .usernameExists(let message):
                            err.message = message
                            completion(false, err)
                        case .limitExceeded(let message):
                            err.message = message
                            completion(false, err)
                        default:
                            completion(false, .unboxerError)
                        }
                    } else if let error = error {
                        err.message = error.localizedDescription
                        
                        switch (error as NSError).code {
                        case ApiErrorCode.noInternet.rawValue:
                            err.code = .noInternet
                            completion(false, err)
                        default:
                            completion(false, .unboxerError)
                        }
                    }
                }
        }
        
    }
}

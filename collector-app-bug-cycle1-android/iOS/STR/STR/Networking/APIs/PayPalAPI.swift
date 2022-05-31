//
//  PayPalAPI.swift
//  STR
//
//  Created by Srujan on 23/07/20.
//  
//

import UIKit
import Alamofire
import Foundation

enum PayPalAPI {
    
    static func getPayPalID(params: JSONDictionary, completionHandler: @escaping
    (String?, ApiError?) -> Void) {
        
        let base64 = (kPayPalClientID + ":" + kPayPalSecret).toBase64()
        
        let headers: HTTPHeaders = ["Authorization": "Basic \(base64)", "Content-Type": "application/x-www-form-urlencoded"]
        
        Alamofire.request(LocalizableString.payPalSandBoxTokensAPI.localizedString, method: .post, parameters: params, headers: headers).responseJSON { response in
            
            if let json = response.result.value {
                PayPalAPI.getAccessToken(dic: json as! JSONDictionary) { (id, error) in
                    if let id = id {
                        completionHandler(id, nil)
                    } else {
                        completionHandler(nil, error)
                    }
                }
            } else {
                let err = ApiError(title: nil, message: LocalizableString.payPalApiFailMsg.localizedString, code: nil)
                completionHandler(nil, err)
            }
        }
    }
    
    static func getAccessToken(dic: JSONDictionary, completionHandler: @escaping
    (String?, ApiError?) -> Void) {
        
        let params = ["grant_type": "refresh_token", "refresh_token": dic["refresh_token"] ?? ""]
        
        let base64 = (kPayPalClientID + ":" + kPayPalSecret).toBase64()
        
        let headers: HTTPHeaders = ["Authorization": "Basic \(base64)", "Content-Type": "application/x-www-form-urlencoded"]
        
        Alamofire.request(LocalizableString.payPalSandBoxTokensAPI.localizedString, method: .post, parameters: params, headers: headers).responseJSON { response in
            
            if let json = response.result.value {
                PayPalAPI.getIdentityID(dic: json as! JSONDictionary) { (id, error) in
                    if let id = id {
                        completionHandler(id, nil)
                    } else {
                        completionHandler(nil, error)
                    }
                }
            } else {
                let err = ApiError(title: nil, message: LocalizableString.payPalApiFailMsg.localizedString, code: nil)
                completionHandler(nil, err)
            }
        }
        
    }
    
    static func getIdentityID(dic: JSONDictionary, completionHandler: @escaping
    (String?, ApiError?) -> Void) {
        
        let headers: HTTPHeaders = ["Authorization": "Bearer \(dic["access_token"] ?? "")", "Content-Type": "application/json"]
        
        Alamofire.request(LocalizableString.payPalSandBoxIdentityAPI.localizedString, method: .get, headers: headers).responseJSON { response in
            
            if let json = response.result.value {
                if let dic = (json as? JSONDictionary), let emails = dic["emails"] as? Array<Any>{
                    if emails.count > 0 {
                        if let emailDic = emails[0] as? JSONDictionary, let email = emailDic["value"], let confirm = emailDic["confirmed"] {
                            if (email as? String)?.count ?? 0 > 0 && (confirm as? Bool ?? false) {
                                completionHandler((email as? String), nil)
                            } else {
                                let err = ApiError(title: nil, message: LocalizableString.payPalApiFailMsg.localizedString, code: nil)
                                completionHandler(nil, err)
                            }
                        } else {
                            let err = ApiError(title: nil, message: LocalizableString.payPalApiFailMsg.localizedString, code: nil)
                            completionHandler(nil, err)
                        }
                    } else {
                        let err = ApiError(title: nil, message: LocalizableString.payPalApiFailMsg.localizedString, code: nil)
                        completionHandler(nil, err)
                    }
                }
            } else {
                let err = ApiError(title: nil, message: LocalizableString.payPalApiFailMsg.localizedString, code: nil)
                completionHandler(nil, err)
            }
        }
    }
}

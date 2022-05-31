//
//  ConsentAPI.swift
//  STR
//
//  Created by Srujan on 14/04/20.
//  
//

import Foundation
import AWSAppSync
import AWSMobileClient

enum ConsentAPI {
    
    static func verifySubject(subjectEmail: String, completionHandler: @escaping (SubjectByStrSubjectEmailQuery.Data.SubjectByStrSubjectEmail.Item?, ApiError?) -> Void) {
        
        AppDelegate.appSyncClient?.fetch(query: SubjectByStrSubjectEmailQuery(subject_email: subjectEmail), cachePolicy: .fetchIgnoringCacheData) { (result, error) in
            
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
            } else if let result = result?.data?.subjectByStrSubjectEmail {
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
    
    static func updateSubject(completionHandler: @escaping (Bool, ApiError?) -> Void) {
        
        let user = Collector.currentCollector
        let consent = ConsentResponse.instance
        let subjectEmail = (ProjectService.instance.currentCollection?.programName ?? "") + "_" + (consent.subjectEmail ?? "")
        let consentDetailsResponse: JSONDictionary = ["response": consent.questionResponse ?? []]
                
        let jsonData = try! JSONSerialization.data(withJSONObject: consentDetailsResponse, options: [])
        let consentDetailsResponseString = String(data: jsonData, encoding: .utf8)!

        let subjectMutation = CreateStrSubjectInput(collectorId: user.userId ?? "", uuid: consent.subjectID ?? "", subjectEmail: subjectEmail, collectorEmail: Collector.currentCollector.email, consentResponse: consentDetailsResponseString, consentVideoId: consent.consentS3UrlPath ?? "", status: "active", programId: ProjectService.instance.currentCollection?.programId, programName: ProjectService.instance.currentCollection?.programName, projectId: ProjectService.instance.currentCollection?.projectId, projectName: ProjectService.instance.currentCollection?.projectName)
            
        AppDelegate.appSyncClient?.perform(mutation: CreateStrSubjectMutation(input: subjectMutation)) { (result, error) in
            
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
    
    static func updateEditConsentDetails(subjectInput: UpdateStrSubjectInput, completionHandler: @escaping (Bool, ApiError?) -> Void) {
                
        AppDelegate.appSyncClient?.perform(mutation: UpdateStrSubjectMutation(input: subjectInput)) { (result, error) in
            
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
    
    static func getConsentQuestionnaire(completionHandler: @escaping ([ListStrConsentQuestionnairesQuery.Data.ListStrConsentQuestionnaire.Item]?, ApiError?) -> Void) {
        
        AppDelegate.appSyncClient?.fetch(query: ListStrConsentQuestionnairesQuery(project_id: ProjectService.instance.currentCollection?.projectId ?? ""), cachePolicy: .fetchIgnoringCacheData) { (result, error) in
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
            } else {
                
                let sortedData = result?.data?.listStrConsentQuestionnaires?.items?.sorted(by: {Int($0?.id ?? "")! < Int($1?.id ?? "")!})
                completionHandler(
                    sortedData as? [ListStrConsentQuestionnairesQuery.Data.ListStrConsentQuestionnaire.Item],
                    nil
                )
            }
        }
    }

    static func revokeConsent(completionHandler: @escaping (Bool, ApiError?) -> Void) {

        let subjectInput = UpdateStrSubjectInput(collectorId: Collector.currentCollector.userId, subjectEmail: Collector.currentCollector.email, collectorEmail: Collector.currentCollector.email, status: "inActive")
        
        AppDelegate.appSyncClient?.perform(mutation: UpdateStrSubjectMutation(input: subjectInput)) { (result, error) in
            
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
}

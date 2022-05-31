//
//  ApiError.swift
//  STR
//
//  Created by Srujan on 10/06/19.
//  
//

import Alamofire
import Foundation

struct ApiError: ErrorPresentable {

    // MARK: - Properties
    var title: String?

    var message: String?
    var code: ApiErrorCode?

    // MARK: - Initializers
    init(title: String? = nil, message: String? = nil, code: ApiErrorCode? = nil) {
        self.title = title
        self.message = message
        self.code = code
    }

    init?(error: Error, data: Data?) {

        if let errorMessage = data?.apiErrorMessage {

            message = errorMessage

            if let code = data?.apiErrorCode, let apiErrorCode = ApiErrorCode(rawValue: code) {
                self.code = apiErrorCode
                
                if Int(CFNetworkErrors.cfurlErrorNotConnectedToInternet.rawValue) == code {
                    title = LocalizableString.offlineError.localizedString
                    message = LocalizableString.checkInternet.localizedString
                }
            }
        }
        else { return nil }
    }

    // MARK: - Utils
    static var defaultError: ApiError {
        return ApiError(
            title: LocalizableString.connectionError.localizedString,
            message: LocalizableString.connectionProblem.localizedString,
            code: nil
        )
    }

    static var unboxerError: ApiError {
        return ApiError(
            title: "Oops!",
            message: "Something Bad Happened!",
            code: ApiErrorCode.decoderError
        )
    }

    static var sessionExpired: ApiError {
        return ApiError(
            title: "Session Expired",
            message: "Please login to continue.",
            code: .sessionExpired
        )
    }

}

enum ApiErrorCode: Int {
    // TODO: Set error Codes
    case ErrorUnknown = 0
    case sampleCase = 1
    case decoderError = 16
    case sessionExpired = 1001
    case accessTokenExpired = 73
    case invalidAccessToken = 1005
    case noInternet = -1009
    case userDoesNotExist = 34
    case invalidToken = 31
}

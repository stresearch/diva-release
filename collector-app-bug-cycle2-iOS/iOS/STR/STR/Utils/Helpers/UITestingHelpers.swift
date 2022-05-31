//
//  UITestingHelpers.swift
//  STR
//
//  Created by Surender on 31/07/19.
//  
//

import Foundation

var isUITesting: Bool { return ProcessInfo.processInfo.arguments.contains("UI-TESTING") }
var isTestingLoginLogoutFlow: Bool {
    return ProcessInfo.processInfo.arguments.contains("LOGIN_LOGOUT_FLOW")
}

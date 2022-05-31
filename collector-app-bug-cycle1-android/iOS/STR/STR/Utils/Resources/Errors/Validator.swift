//
//  Validator.swift
//  STR
//
//  Created by Srujan on 10/06/19.
//  
//

import Foundation

struct ValidationError: ErrorPresentable {
    var title: String?
    var message: String?

    init(title: String? = nil, message: String? = nil) {
        self.title = title
        self.message = message
    }
}

enum Validator {

    enum Pattern: String {
        case email = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}"
        case name = "^([ \\u00c0-\\u01ffa-zA-Z'\\-])+$"
        case password = "^(?=.*[A-Z])(?=.*[!@#$&*])(?=.*[0-9])(?=.*[a-z]).{8,64}$" //"^(?=.*[A-Za-z])((?=.*\\d)|(?=.*[A-Z])|(?=.*\\W)).{8,}$"
        case timerName = "^([ \\u00c0-\\u01ffa-zA-Z'\\-])+$.{,20}"
    }

    static func valid(value: String, inPattern pattern: Pattern) -> Bool {
        return valid(value: value, inPattern: pattern.rawValue)
    }

    static func valid(value: String, inPattern pattern: String) -> Bool {
        let range = value.range(of: pattern, options: .regularExpression)

        return range != nil
    }
}

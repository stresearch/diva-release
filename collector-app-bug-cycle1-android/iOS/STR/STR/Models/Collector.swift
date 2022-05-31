//
//  User.swift
//  SafePassageDriver
//
//  Created by Srujan on 10/06/19.
//  

import CoreData

class Collector {
    
    // MARK: - Properties
    var userId: String!
    var firstName: String!
    var lastName: String!
    var email: String!
    var consentSetUp = false
    var dropBoxSetUp = false
    var payPalSetUp = false
    var dropBoxToken: String!
    var payPalID: String!
    
    var uploadedCount: String!
    var verifiedCount: String!
    var notVerifiedCount: String!
    var consentedCount: String!
    var authorized: String!
    var outstandingAmount: String!
    
    static let currentCollector = Collector()
    
    private init() {}
    
    func clear() {
        userId = nil
        firstName = nil
        lastName = nil
        email = nil
        consentSetUp = false
        dropBoxSetUp = false
        payPalSetUp = false
        dropBoxToken = nil
        payPalID = nil
        
        uploadedCount = nil
        verifiedCount = nil
        notVerifiedCount = nil
        consentedCount = nil
        authorized = nil
        outstandingAmount = nil
    }
    
    private struct JSONKey {
        static let userId = "sub"
        static let firstName = "custom:first_name"
        static let lastName = "custom:last_name"
        static let email = "email"
    }
    
    func setUserDetails(dict: JSONDictionary) {
        self.userId = dict[JSONKey.userId] as? String
        self.email = dict[JSONKey.email] as? String
        self.firstName = dict[JSONKey.firstName] as? String
        self.lastName = dict[JSONKey.lastName] as? String
    }
    
    func setCollectorDetails(item: ListStrCollectorsQuery.Data.ListStrCollector.Item) {
        self.userId = item.collectorId
        self.email = item.collectorEmail
        self.firstName = item.firstName
        self.lastName = item.lastName
        self.consentSetUp = item.isConsented ?? false
        self.dropBoxSetUp = item.isDropboxIntegrated ?? false
        self.payPalSetUp = item.isPaypalIntegrated ?? false
        self.dropBoxToken = item.dropboxToken
        self.payPalID = item.paypalEmailId
        
        self.uploadedCount = item.uploadedCount
        self.verifiedCount = item.verifiedCount
        self.notVerifiedCount = item.notVerifiedCount
        self.consentedCount = item.consentedCount
        self.authorized = item.authorized
        self.outstandingAmount = item.outstandingAmount
    }
    
    func copy(with zone: NSZone? = nil) -> Any {
        let copy = Collector()
        copy.userId = Collector.currentCollector.userId
        copy.email = Collector.currentCollector.email
        copy.firstName = Collector.currentCollector.firstName
        copy.lastName = Collector.currentCollector.lastName
        copy.consentSetUp = Collector.currentCollector.consentSetUp
        copy.dropBoxSetUp = Collector.currentCollector.dropBoxSetUp
        copy.payPalSetUp = Collector.currentCollector.payPalSetUp
        copy.dropBoxToken = Collector.currentCollector.dropBoxToken
        copy.payPalID = Collector.currentCollector.payPalID
        
        copy.uploadedCount = Collector.currentCollector.uploadedCount
        copy.verifiedCount = Collector.currentCollector.verifiedCount
        copy.notVerifiedCount = Collector.currentCollector.notVerifiedCount
        copy.consentedCount = Collector.currentCollector.consentedCount
        copy.authorized = Collector.currentCollector.authorized
        copy.outstandingAmount = Collector.currentCollector.outstandingAmount
        
        return copy
    }
    
    func revert(copy: Collector) {
        self.userId = copy.userId
        self.email = copy.email
        self.firstName = copy.firstName
        self.lastName = copy.lastName
        self.consentSetUp = copy.consentSetUp
        self.dropBoxSetUp = copy.dropBoxSetUp
        self.payPalSetUp = copy.payPalSetUp
        self.dropBoxToken = copy.dropBoxToken
        self.payPalID = copy.payPalID
        
        self.uploadedCount = copy.uploadedCount
        self.verifiedCount = copy.verifiedCount
        self.notVerifiedCount = copy.notVerifiedCount
        self.consentedCount = copy.consentedCount
        self.authorized = copy.authorized
        self.outstandingAmount = copy.outstandingAmount
    }
}

extension Collector {
    
    //MARK: Validation
    func isValid() throws -> Bool {
//        try _ = User.valid(firstName: firstName)
//        try _ = User.valid(lastName: lastName)
//        try _ = User.validEmail(email: email)
//        try _ = User.validPassword(password: "")
        return true
    }
    
    class func valid(firstName: String?) throws -> Bool {

        guard let firstName = firstName?.removingWhitespaces(), !firstName.isEmpty else {
            throw ValidationError(message: LocalizableString.firstNameMandatory.localizedString)
        }

        guard Validator.valid(value: firstName, inPattern: .name) else {
            throw ValidationError(message: LocalizableString.firstNameInvalid.localizedString)
        }

        return true
    }

    class func valid(lastName: String?) throws -> Bool {

        guard let lastName = lastName?.removingWhitespaces(), !lastName.isEmpty else {
            throw ValidationError(message: LocalizableString.lastNameMandatory.localizedString)
        }

        guard Validator.valid(value: lastName, inPattern: .name) else {
            throw ValidationError(message: LocalizableString.lastNameInvalid.localizedString)
        }

        return true
    }
    
    class func validEmail(email: String?) throws -> Bool {

        guard let email = email, !email.isEmpty else {
            throw ValidationError(message: LocalizableString.emailMandatory.localizedString)
        }

        guard Validator.valid(value: email.lowercased(), inPattern: .email) else {
            throw ValidationError(message: LocalizableString.emailInvalid.localizedString)
        }

        return true
    }
    
    class func validPassword(password: String?) throws -> Bool {

        guard let password = password, !password.isEmpty else {
            throw ValidationError(message: LocalizableString.passwordMandatory.localizedString)
        }

        if !Validator.valid(value: password, inPattern: .password) {
            throw ValidationError(message: LocalizableString.passwordInvalid.localizedString)
        }

        return true
    }
    
    class func validPasswordMatch(password: String?, passwordConfirmation: String?) throws -> Bool {

        guard let password = password, !password.isEmpty else {
            throw ValidationError(message: LocalizableString.passwordMandatory.localizedString)
        }

        if !Validator.valid(value: password, inPattern: .password) {
            throw ValidationError(message: LocalizableString.passwordInvalid.localizedString)
        }

        guard let passwordConfirmation = passwordConfirmation, !passwordConfirmation.isEmpty else {
            throw ValidationError(message: LocalizableString.confirmPassMandatory.localizedString)
        }

        if password != passwordConfirmation {
            throw ValidationError(message: LocalizableString.passwordMatch.localizedString)
        }
        return true
    }
}

class UserSignUpData {

    lazy var firstName: String = ""
    lazy var lastName: String = ""
    lazy var email: String = ""
    lazy var password: String = ""
    lazy var confirmPassword: String = ""

    // MARK: - Validations
    func isValid() throws -> Bool {
        try _ = Collector.valid(firstName: firstName)
        try _ = Collector.valid(lastName: lastName)
        try _ = Collector.validEmail(email: email)
        try _ = Collector.validPassword(password: password)
        try _ = Collector.validPasswordMatch(password: password, passwordConfirmation: confirmPassword)

        return true
    }

    func isEditProfileValid() throws -> Bool {
        try _ = Collector.valid(firstName: firstName)
        try _ = Collector.valid(lastName: lastName)

        return true
    }
    
}

class UserSignInData {
    lazy var email: String = ""
    lazy var password: String = ""
    
    // MARK: - Validations
    func isValid() throws -> Bool {
        try _ = Collector.validEmail(email: email)
        try _ = Collector.validPassword(password: password)

        return true
    }
}

class UserChangePasswordData {
    lazy var currentPassword = ""
    lazy var newPassword = ""
    lazy var confirmPassword = ""
    
    //MARK: Validations
    func isValid() throws -> Bool {
        try _ = Collector.validPassword(password: currentPassword)
        try _ = Collector.validPassword(password: newPassword)
        try _ = Collector.validPasswordMatch(password: newPassword, passwordConfirmation: confirmPassword)
        
        return true
    }
}

class UserConfirmPasswordData {

    lazy var otp = ""
    lazy var newPassword = ""
    lazy var confirmPassword = ""
    
    //MARK: Validations
    func isValid() throws -> Bool {
        try _ = self.validOtp(otp: otp)
        try _ = Collector.validPassword(password: newPassword)
        try _ = Collector.validPassword(password: confirmPassword)
        try _ = Collector.validPasswordMatch(password: newPassword, passwordConfirmation: confirmPassword)
        
        return true
    }
    
    func validOtp(otp: String?) throws -> Bool {

        guard let otp = otp?.removingWhitespaces(), !otp.isEmpty else {
            throw ValidationError(message: LocalizableString.otpMandatory.localizedString)
        }

        return true
    }
}

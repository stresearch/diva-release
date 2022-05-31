//
//  LocalizableString.swift
//  STR
//
//  Created by Srujan on 10/06/19.
//  
//

import Foundation

enum LocalizableString: String {
    //MARK: - Global
    case ok = "OK"
    case cancel = "Cancel"
    case connectionError = "Connection error"
    case connectionProblem = "There was a problem, please try again."
    case offlineError = "Offline error"
    case checkInternet = "The Internet connection appears to be offline."
    case userDoesNotExist = "User does not exist"
    case invalidToken = "Your Session has been Expired"
    case continueText = "Continue"
    case logout = "Log out"
    case delete = "Delete"
    case back = " Back"
    case yes = "Yes"
    case no = "No"
    case done = "Done"
    case close = "Close"
    case logoutMessage = "Are you sure you want to log out?"
    case openSettings = "Open Settings"
    case locationDisable = "Location Access Disabled"
    case locationDisableMessage
        = "Turn on Location Services to allow this App to determine your location."
    case success = "Success"
    case message = "Message"
    case alert = "Alert"
    
    //MARK: Support
    case supportEmail = "t"
    case subject = "d"

    // MARK: - Home
    case consent = "consent"
    case consentTypoAlert = "New subject email entered - Would you like to consent?"
    
    // MARK: - Register
    case firstName = "First name"
    case lastName = "Last name"
    case email = "Email"
    case phone = "Contact Number"
    case password = "Password"
    case username = "User name"
    case company = "Company Code"
    case confirmPassword = "Confirm password"
    case firstNameInvalid = "First name contains invalid characters."
    case firstNameMandatory = "First name field is mandatory."
    case lastNameInvalid = "Last name contains invalid characters."
    case lastNameMandatory = "Last name field is mandatory."
    case emailInvalid = "Please enter email in the valid format."
    case emailMandatory = "Email field is mandatory."
    case phoneInvalid = "Invalid phone number."
    case phoneMandatory = "Phone field is mandatory."
    case passwordInvalid
        = "Password should be alpha-numeric with minimum 8 to 64 characters, 1 uppercase and lowercase, at least 1 number and 1 Special Character"
    case passwordMandatory = "Password field is mandatory."
    case passwordMatch = "Passwords don’t match."
    case confirmPassMandatory = "Confirm password field is mandatory."
    case completeAllFields = "Please complete all fields."
    case termsAccepted = "You must agree to the terms and conditions before registering."
    case profileUpdate = "Profile Updated Successfully"
    case verifyEmail = "A verification email has been sent to "
    
    //MARK: Change Username
    case changeUsername = "Change username"
    case newEmail = "New Email"
    case confirmEmail = "Confirm Email"
    case enterPassword = "Enter Password"
    
    //MARK: Change Password
    case changePassword = "Change Password"
    case currentPassword = "Current Password"
    case passwordSuccessMessage = "Password changed successfully"
    
    //MARK: Confirm Forgot Password
    case otp = "Otp"
    case resendOtp = "RESEND OTP"
    case otpMandatory = "OTP is mandatory"
    case otpInvalid = "Invalid OTP"
    case otpStaticText = "One Time Password (OTP) has been sent to your email ******@"
    
    //MARK: - Password
    case successPassMessage = "An Email has been sent to recover your password."

    //MARK: Edit Consent
    case datasetRelease = "Dataset Release"
    case faceDetection = "Face recognition usage"
    case retainmentTime = "Retention"
    case datasetReleaseKey = "DataSetResponse"
    case faceDetectionKey = "FaceRecognition"
    
    //MARK: Traning Video
    case trainingVideoOverLayText = "Your video should look similar to this example video.  Please collect from above looking down and keep your subject tight in the box."
    
    //MARK: Consent Email
    case consentOverLayText = #"Please select the record button, say "I consent to this video collection""#
    case subjectInActive = "This subject is not allowed"
    case consentEmailLearnMore = "Please enter or select your subject's email address.  If your subject has not yet consented for video recording, then we will guide them through the consent process with a sequence of questions to specify how we should handle their private data.  This is a one time procedure for each new subject, and the answers can be modified at any time."
    
    // MARK: - Contacts
    case contacts = "Contacts"
    case add = "Add"
    case notifications = "Notifications"
    case apply = "Apply"
    case push = "Push"
    case edit = "Edit"
    case none = "None"
    case saveChanges = "Are you sure you want to go back without saving the changes?"
 
    //MARK: -
    case noLocation = "Location not available."

    // MARK:-
    case startDate = "Start Date"
    case endDate = "End Date"
    case startTime = "Start Time"
    case endTime = "End Time"
    case selectDate = "Select Date"
    case selectTime = "Select Time"
    case endDateBigError = "Start Date should be less than End Date."
    case startDateSmallError = "Start date or time should be ahead of Current date or time."


    //MARK: - Settings
    case settings = "Settings"
    case profile = "Profile"
    case faq = "FAQ"
    case feedback = "Send Feedback"
    case terms = "Terms and conditions"
    case privacy = "Privacy policy"
    case about = "About"
    case appVersion = "App Version"

    //MARK: - Profile
    case editProfile = "Edit Profile"
    case oldPassword = "Old password"
    case newPassword = "New password"
    case confirmNewPassword = "Confirm new password"
    case profileUpdated = "Profile Updated"
    case oldPassNameMandatory = "Old password field is mandatory."
    case newPassMandatory = "New password field is mandatory."
    case update = "Update"
    case revokeConsentMessage = "This will delete all your videos containing you as a subject. This cannot be undone. Do you wish to proceed"
    
    case collectionVideoDeleteMessage = "Are you sure want to discard this drafted video"
    
    case collectorCollectionsMappingEmptyList = "No collections found"
    case collectorProjectsMappingEmptyList = "No projects found"

    //MARK: URLs
    case aboutURL = "https://visym.com/collector"  // JEBYRNE
    case termsURL = "http://visym.com/legal.html"
    case privacyURL = "q"
    case faqURL = "http://visym.com/faq.html"
    
    //MARK: S3 bucket Name
    case bucketNamePointingToProd = ""
    case bucketNamePointingToTest = "visym-data-lake213217-visymcdev"
    case bucketNamePointingToDev = "visym-data-lake213217-visymcodev"
    case bucketPublicName = "visym-public-data213217-visymcdev"
    
    //MARK: S3 bucket Url
    //case s3BucketUrl = "https://s3.amazonaws.com/diva-prod-data-lake174516-visym/"
    case s3BucketUrl = "https://s3.amazonaws.com/visym-data-lake213217-visymcdev/"
    case s3BucketPublicUrl = "https://s3.amazonaws.com/visym-public-data213217-visymcdev/"
    
    //MARK: S3 Bucket Folder Name
    case consentS3FolderStructure = "uploads/Consent_Documentations/"
    case recordVideoS3FolderStructure = "uploads/Programs/"
    
    //MARK: PayPal
    case payPalRedirectURI = "collectordeeplink://logincallback?"
    case payPalUrlSchema = "collectordeeplink"
    case payPalSandBoxConnectURL1 = "https://www.sandbox.paypal.com/connect/?flowEntry=static&client_id="
    case payPalConnectURL2 = "&scope=email&redirect_uri="
    case payPalSandBoxTokensAPI = "https://api.sandbox.paypal.com/v1/oauth2/token"
    case payPalSandBoxIdentityAPI = "https://api.sandbox.paypal.com/v1/identity/oauth2/userinfo?schema=paypalv1.1"
    case payPalLiveConnectURL1 = "https://www.paypal.com/connect/?flowEntry=static&client_id="
    case payPalLiveTokensAPI = "https://api.paypal.com/v1/oauth2/token"
    case payPalLiveIdentityAPI = "https://api.paypal.com/v1/identity/oauth2/userinfo?schema=paypalv1.1"
    case payPalApiFailMsg = "Something went wrong. Please try again"
    
    case dropBoxUrlSchema = "db-jh6u8qe1aqnun7q"
    
    //MARK: Rating Videos
    case noRatingVideosMsg = " You have completed all ratings assigned to you.  Nice job!"
    
    var localizedString: String { return NSLocalizedString(rawValue, comment: "") }
}

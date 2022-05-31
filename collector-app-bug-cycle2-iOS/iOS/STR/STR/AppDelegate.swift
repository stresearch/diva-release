//
//  AppDelegate.swift
//  STR
//
//  Created by Srujan on 31/12/19.
//  
//

import UIKit
import IQKeyboardManagerSwift
import AWSS3
import AWSAppSync
import AWSMobileClient
import SwiftyDropbox
import SafariServices

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
    
    var window: UIWindow?
    var orientationLock = UIInterfaceOrientationMask.all
    static var appSyncClient: AWSAppSyncClient?
    static var s3BucketName: String?
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
        IQKeyboardManager.shared.enable = true
        
        DropboxClientsManager.setupWithAppKey(kDropBoxAppKey)
        pullS3BucketName()
        configureSideMenu()        
        
        // Initialize AWS AppSync
        self.initializeAwsAppSync()
        
        let value = UIInterfaceOrientation.portrait.rawValue
        UIDevice.current.setValue(value, forKey: "orientation")
        AppUtility.lockOrientation(.portrait)
        return true
    }
    
    func application(_ application: UIApplication, supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
            return self.orientationLock
    }
    
    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        
        if url.scheme == LocalizableString.payPalUrlSchema.localizedString {
            
            if let authorizationCode = url.absoluteString.getQueryStringParameter(param: "code") {
                ProfileViewController.payPalAuthCode = authorizationCode
                ProfileViewController.isConnectingToPayPal = true
                ProfileViewController.isConnectingToDropBox = false
                Collector.currentCollector.payPalSetUp = true
                print("authorizationCode: \(authorizationCode)")
            }
            
            if let topVC = (((self.window?.rootViewController)?.children.first as? UITabBarController)?.selectedViewController as? UINavigationController)?.visibleViewController as? SFSafariViewController {
                topVC.dismiss(animated: true, completion: nil)
            } else if let topVC = (self.window?.rootViewController as? UINavigationController)?.visibleViewController {
                topVC.dismiss(animated: true, completion: nil)
            }
            
        } else if url.scheme == LocalizableString.dropBoxUrlSchema.localizedString {
            
            let oauthCompletion: DropboxOAuthCompletion = {
                if let authResult = $0 {
                switch authResult {
                   case .success(let accessToken):
                       ProfileViewController.isConnectingToDropBox = true
                       ProfileViewController.isConnectingToPayPal = false
                       Collector.currentCollector.dropBoxSetUp = true
                       Collector.currentCollector.dropBoxToken = accessToken.accessToken
                       NotificationCenter.default.post(name: .dropBox, object: nil)
                   case .cancel:
                       print("DropBox uthorization flow was manually canceled by user!")
                   case .error(_, let description):
                    print("Error: \(String(describing: description))")

                   }
                }
            }
            DropboxClientsManager.handleRedirectURL(url, completion: oauthCompletion)
        }
        return true
    }
    
    private func initializeAwsAppSync() {

        do {
            
            // You can choose the directory in which AppSync stores its persistent cache databases
        //let cacheConfiguration = try AWSAppSyncCacheConfiguration()

            // Initialize the AWS AppSync configuration
            let appSyncConfig = try AWSAppSyncClientConfiguration(appSyncServiceConfig: AWSAppSyncServiceConfig(),
                                                                  userPoolsAuthProvider: {
                                                                    class MyCognitoUserPoolsAuthProvider : AWSCognitoUserPoolsAuthProviderAsync {
                                                                        func getLatestAuthToken(_ callback: @escaping (String?, Error?) -> Void) {
                                                                            AWSMobileClient.default().getTokens { (tokens, error) in
                                                                                if error != nil {
                                                                                    callback(nil, error)
                                                                                } else {
                                                                                    callback(tokens?.idToken?.tokenString, nil)
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                    return MyCognitoUserPoolsAuthProvider()}(),
                                                                    cacheConfiguration: nil)
            
            // Initialize the AWS AppSync client
            AppDelegate.appSyncClient = try AWSAppSyncClient(appSyncConfig: appSyncConfig)
        } catch {
            print("Error initializing appsync client. \(error)")
        }
        
    }
    
    private func pullS3BucketName() {
        if let projectJsonData = Utilities.getDataFromLocalJSON(filename: "awsconfiguration") {
            let jsonResult = try? JSONSerialization.jsonObject(with: projectJsonData, options: .mutableLeaves)
            if let jsonResult = jsonResult as? Dictionary<String, AnyObject> {
                let val = jsonResult["S3TransferUtility"]  as? Dictionary<String, AnyObject>
                let val1 = val?["Default"]
                let bucketName = val1?["Bucket"] ?? ""
                
                AppDelegate.s3BucketName = bucketName
            }
        }
    }
    
    private func configureSideMenu() {
        MenuController.preferences.basic.menuWidth = UIScreen.main.bounds.width - 40
        
        print("UIScreen.main.bounds.width---\(UIScreen.main.bounds.width)")
        
        MenuController.preferences.basic.defaultCacheKey = "0"
    }
    
    static func clearAppSyncCache() {
        do {
            try AppDelegate.appSyncClient?.clearCaches()
        } catch {
            print(error.localizedDescription)
        }
    }
}


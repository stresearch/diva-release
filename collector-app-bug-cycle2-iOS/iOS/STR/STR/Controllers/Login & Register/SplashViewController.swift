//
//  SplashViewController.swift
//  STR
//
//  Created by Srujan on 01/04/20.
//  
//

import UIKit
import AWSMobileClient
import AWSAppSync

class SplashViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
    }
    
    override func viewWillAppear(_ animated: Bool) {
        self.navigationController?.navigationBar.isHidden = true
        self.checkUserStatus()
        
    }

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

    private func checkUserStatus() {

        AWSMobileClient.default().initialize { (state, error) in
            if let error = error {
                print(error.localizedDescription)
                SplashViewController.clearDefaultsValue()
                AWSMobileClient.default().signOut()
                self.navigateToAuthVC()
            } else if let state = state {
                switch state {
                case .signedIn:
                    self.getUserInfo()
                    self.checkForInvalidTokens()
                case .signedOut:
                    self.navigateToAuthVC()
                default:
                    self.navigateToAuthVC()
                }
            }
        }
    }
    
    private func checkForInvalidTokens() {
        AWSMobileClient.default().addUserStateListener(self) { (state, dic) in
             switch state {
             case .signedOutFederatedTokensInvalid, .signedOutUserPoolsTokenInvalid:
                SplashViewController.clearDefaultsValue()
                 AWSMobileClient.default().signOut()
             default: break
             }
         }
    }
    
    private func getUserInfo() {
        
        UserAPI.getProfile { (status, err) in
            if status {
                SignUpViewController.goToHome()
            } else if let error = err {
                switch error.code {
                case .noInternet:
                    self.presentDefaultAlertWithError(error: error, animated: true, completion: nil)
                default:
                    UIUtilities.showAlertMessageWithActionHandler("", message: error.message ?? "", buttonTitle: LocalizableString.ok.localizedString, viewControllerUsed: self) {
                        SplashViewController.clearDefaultsValue()
                        AWSMobileClient.default().signOut()
                        self.navigateToAuthVC()
                    }
                }
            }
        }
    }
    
    private func navigateToAuthVC() {
        guard let authVC = self.storyboard?
            .instantiateViewController(withIdentifier: AuthViewController.storyboardIdentifier)
            else { return }
        self.navigationController?.pushViewController(authVC, animated: true)
    }
    
    static func clearDefaultsValue() {
        ConsentResponse.instance.clear()
        Collector.currentCollector.clear()
        ProjectService.instance.clear()
        UserDefaults.standard.reset()
    }
}

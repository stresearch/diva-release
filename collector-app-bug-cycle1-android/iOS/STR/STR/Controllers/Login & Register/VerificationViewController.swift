//
//  VerificationViewController.swift
//  STR
//
//  Created by Srujan on 06/02/20.
//  
//

import UIKit

class VerificationViewController: UIViewController {

    //MARK: Outlets
    @IBOutlet weak var labelEmail: UILabel!
    @IBOutlet weak var buttonResend: UIButton!
    
    var userEmail: String?
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        updateUI()
        updateVerifyEmailText()
        self.navigationController?.navigationBar.isHidden = true
    }
    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

    private func updateVerifyEmailText() {
        let attributedString: NSMutableAttributedString = NSMutableAttributedString(string: LocalizableString.verifyEmail.localizedString + (userEmail ?? ""))
        attributedString.setColorForText((userEmail ?? ""), with: UIColor.appColor(.main) ?? .black)
        labelEmail.attributedText = attributedString
    }
    
    private func updateUI() {
        buttonResend.borderColor = .black
        buttonResend.borderWidth = 2.0
    }
    
    //MARK: Actions
    @IBAction func closeBtnAction(_ sender: UIButton) {
        navigateToAuthVC()
    }
    
    @IBAction func resendBtnAction(_ sender: UIButton) {
        self.showProgress()
        self.requestForResendVerification()
    }
    
    private func navigateToAuthVC() {
        guard let signInVC = self.storyboard?
            .instantiateViewController(withIdentifier: SignInViewController.storyboardIdentifier) as? SignInViewController
            else { return }
        signInVC.valFromAuth = false
        self.navigationController?.pushViewController(signInVC, animated: true)
    }
}

//MARK: API Calls
extension VerificationViewController {
    
    private func requestForResendVerification() {
        
        let email = UserDefaults.standard.value(forKey: UserDefaults.Key.login.rawValue) as? String
        AuthAPI.resendVerificationEmail(email: email ?? "") { (status, error) in
            self.hideProgress()
            if status {
                
            } else if let error = error {
                self.presentDefaultAlertWithError(error: error, animated: true, completion: nil)
            }
        }
    }
}

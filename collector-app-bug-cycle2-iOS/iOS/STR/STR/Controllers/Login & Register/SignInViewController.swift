//
//  SignInViewController.swift
//  STR
//
//  Created by Srujan on 31/12/19.
//  
//

import UIKit

enum SignInTableRow: TableRow {
    
    case username, password
    
    var title: String {
        switch self {
        case .password: return LocalizableString.password.localizedString
        case .username: return LocalizableString.username.localizedString
        }
    }
}

class SignInViewController: UIViewController {

    // MARK:- Outlets
    
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var buttonClose: UIButton!
    
    fileprivate let tableRows: [SignInTableRow] = [
        .username, .password
    ]
    private var changePasswordView: ChangeUserNameView?
    lazy var signInData = UserSignInData()
    lazy var changePasswordData = UserChangePasswordData()
    
    var valFromAuth = true
    
    override func viewDidLoad() {
        super.viewDidLoad()

        setupTableView()
        if valFromAuth {
            self.buttonClose.isHidden = false
        }
        else {
            self.buttonClose.isHidden = true
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        if let email = UserDefaults.standard.value(forKey: UserDefaults.Key.login.rawValue) {
            signInData.email = email as? String ?? ""
        }
        self.navigationController?.navigationBar.isHidden = true
    }
    
    // MARK:- UI Utils
    private func setupTableView() {
        self.tableView.dataSource = self
        self.tableView.delegate = self
        // Update the table header view height to center the fields
        if let tableHeaderView = tableView.tableHeaderView {
            var frame = tableHeaderView.frame
            frame.size.height = self.view.frame.height / 7
            tableHeaderView.frame = frame
            tableView.tableHeaderView = tableHeaderView
            tableHeaderView.setNeedsLayout()
            tableHeaderView.layoutIfNeeded()
        }
    }
    
    private func navigationToHomeScreen() {
        SignUpViewController.goToHome()
    }
    
    private func navigateToVerificationScreen() {
        let verifyVC: VerificationViewController? = self.storyboard?.instantiateViewController()
        if verifyVC != nil {
            verifyVC?.userEmail = signInData.email
            self.navigationController?.pushViewController(verifyVC!, animated: true)
        }
    }
    
    private func navigateToSetUpScreen() {
        
        if !Collector.currentCollector.payPalSetUp || !Collector.currentCollector.dropBoxSetUp {
            let setUpVC: SetUpViewController? = self.storyboard?.instantiateViewController()
            if setUpVC != nil {
                self.navigationController?.pushViewController(setUpVC!, animated: true)
            }
        } else {
            self.navigationToHomeScreen()
        }
    }
    
    private func navigateToChangePasswordScreen() {
        setUpCustomView()
    }
    
    private func setUpCustomView() {
        
        guard let usernameView = ChangeUserNameView.instanceFromNib()
            else { return }
        usernameView.isUsername = false
        usernameView.addTextFieldTarget()
        usernameView.delegate = self
        changePasswordView = usernameView
        self.view.addSubview(usernameView)
        usernameView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            usernameView.trailingAnchor.constraint(equalTo: self.view.trailingAnchor),
            usernameView.leadingAnchor.constraint(equalTo: self.view.leadingAnchor),
            usernameView.topAnchor.constraint(equalTo: self.view.topAnchor),
            usernameView.bottomAnchor.constraint(equalTo: self.view.bottomAnchor)
        ])
    }
    
    private func removeCustomView() {
        if let changePasswordView = self.changePasswordView {
            UIView.animate(withDuration: 0.5, delay: 0, options: [.curveEaseOut], animations: { [weak self] in
                guard let self = self else {return}
                changePasswordView.removeFromSuperview()
                self.view.layoutIfNeeded()
                }, completion: { (finished) in
                    self.changePasswordData = UserChangePasswordData()
            })
        }
    }
    
    // MARK:- Actions
    
    @IBAction func signInBtnPressed(_ sender: UIButton) {
//        SignUpViewController.goToHome()
        do {
            _ = try signInData.isValid()
            self.requestForLoginUser()
        } catch let err as ValidationError {
            self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
        } catch {
            
        }
    }
    
    @IBAction func backBtnPressed(_ sender: UIButton) {
        self.navigationController?.popViewController(animated: true)
    }
    
    @IBAction func signUpBtnPressed(_ sender: UIButton) {
        let signupVC: SignUpViewController? = self.storyboard?.instantiateViewController()
        if signupVC != nil {
             self.navigationController?.pushViewController(signupVC!, animated: true)
        }
    }

    @IBAction func forgotPasswordBtnPressed(_ sender: UIButton) {
        let forgotPassVC: ForgotPasswordViewController? = self.storyboard?.instantiateViewController()
        if forgotPassVC != nil {
             self.navigationController?.pushViewController(forgotPassVC!, animated: true)
        }
    }

}

extension SignInViewController: UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return tableRows.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        if let cell = tableView.dequeueReusableCell(withIdentifier: SingleFieldTableViewCell.reuseIdentifier, for: indexPath) as? SingleFieldTableViewCell {
            let curentRow = tableRows[indexPath.row]
            cell.currentRow = curentRow
            cell.delegate = self
            cell.textField.keyboardType = .default
            switch curentRow {
            case .password:
                cell.textField.isSecureTextEntry = true
            case .username:
                cell.textField.text = signInData.email
                cell.textField.isSecureTextEntry = false
                cell.textField.keyboardType = .emailAddress
            }
            return cell
        }
        return UITableViewCell()
    }
    
}

extension SignInViewController: UITableViewDelegate {
        
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 90
    }

}

extension SignInViewController {
    
    private func requestForLoginUser() {
        
        self.showProgress()
        
        AuthAPI.loginUser(email: signInData.email, password: signInData.password) { [weak self] (status, error) in
            
            guard let self = self else { return }
                        
            if status {
                
                UserDefaults.standard.set(self.signInData.email, forKey: UserDefaults.Key.login.rawValue)
                
                UserAPI.getProfile { [weak self] (status, error) in
                    guard let self = self else { return }                    
                    
                    if status {
                        /*if Collector.currentCollector.consentSetUp && Collector.currentCollector.dropBoxSetUp && Collector.currentCollector.payPalSetUp {
                            self.hideProgress()
                            self.navigationToHomeScreen() //TBD
                        } else if !Collector.currentCollector.consentSetUp {
                            self.requestForSubjectVerification { status in
                                self.hideProgress()
                                if status {
                                    self.requestForUpdateProfie(consentSetUp: true)
                                    self.navigateToSetUpScreen()
                                } else {
                                    self.requestForUpdateProfie(consentSetUp: false)
                                    self.navigateToSetUpScreen()
                                }
                            }
                        } else {
                            self.hideProgress()
                            self.navigateToSetUpScreen()
                        }*/
                        
                        if Collector.currentCollector.consentSetUp {
                            self.hideProgress()
                            self.navigationToHomeScreen() //TBD
                        } else {
                            self.requestForSubjectVerification { status in
                                self.hideProgress()
                                if status {
                                    self.requestForUpdateProfie(consentSetUp: true)
                                    self.navigationToHomeScreen()
                                } else {
                                    self.requestForUpdateProfie(consentSetUp: false)
                                    self.navigationToHomeScreen()
                                }
                            }
                        }
                        
                    } else {
                        self.hideProgress()
                        self.navigationToHomeScreen()
                    }
                }
            } else if let error = error {
                /// Show alert with error
                self.hideProgress()
                self.presentDefaultAlertWithError(error: error, animated: true, completion: nil)
            }
        }
    }
    
    private func requestForSubjectVerification(completionHandler: @escaping (Bool) -> Void) {
        
        ConsentAPI.verifySubject(subjectEmail: Collector.currentCollector.email) { [weak self] (data, error) in
            guard let self = self else { return }
            if let _ = error {
                completionHandler(false)
            } else if let result = data {
                if result.uuid != Collector.currentCollector.userId {
                    self.requestForUpdateSubjectInfo(subjectEmail: result.subjectEmail, collecotEmail: result.collectorEmail)
                }
                completionHandler(true)
            } else {
                completionHandler(false)
            }
        }
        
    }
    
    private func requestForUpdateSubjectInfo(subjectEmail: String, collecotEmail: String) {
        
        let updateSubjectDevInput = UpdateStrSubjectInput(uuid: Collector.currentCollector.userId, subjectEmail: subjectEmail, collectorEmail: collecotEmail)
        
        self.showProgress()
        
        ConsentAPI.updateEditConsentDetails(subjectInput: updateSubjectDevInput) { (_, _) in
            
        }
    }
    
    private func requestForChangePassword() {
        
        self.showProgress()

        AuthAPI.changePassword(oldPassword: changePasswordData.currentPassword, newPassword: changePasswordData.newPassword) { (status, error) in
            self.hideProgress()
            if status {
                
            } else if let error = error {
                /// Show alert with error
                self.presentDefaultAlertWithError(error: error, animated: true, completion: nil)
            }
        }
    }
    
    private func requestForUpdateProfie(consentSetUp: Bool) {
        
        let data = UserSignUpData()
        data.firstName = Collector.currentCollector.firstName
        data.lastName = Collector.currentCollector.lastName
        
        Collector.currentCollector.consentSetUp = consentSetUp
        
        UserAPI.updateProfileToDDB(params: data) { (_, _) in
            
        }
    }
}

extension SignInViewController: SingleFieldTableViewCellDelegate {

    func didChangeValue(for row: TableRow, value: String) {
        guard let row = row as? SignInTableRow
            else { return }
        switch row {
        case .username:
            signInData.email = value
        case .password:
            signInData.password = value
        }
    }

}

extension SignInViewController: ChangeUserNameViewDelegate {
    
    func didChangeTextfieldValue(for row: TableRow, value: String) {
        guard let row = row as? ChangePassword
            else { return }
        switch row {
        case .currentPassword:
            changePasswordData.currentPassword = value
        case .newPassword:
            changePasswordData.newPassword = value
        case .confirmPassword:
            changePasswordData.confirmPassword = value
        }
    }
    
    func didTapOnSubmit() {
        do {
            _ = try changePasswordData.isValid()
            self.requestForChangePassword()
        } catch let err as ValidationError {
            self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
        } catch {
            
        }
    }
    
    func didTapOnCancel() {
        removeCustomView()
    }
}

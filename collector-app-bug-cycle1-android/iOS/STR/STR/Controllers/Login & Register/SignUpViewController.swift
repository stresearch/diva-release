//
//  SignUpViewController.swift
//  STR
//
//  Created by Srujan on 31/12/19.
//  
//

import UIKit
import AWSMobileClient

public protocol TableRow {
    var title: String {get} 
}

enum SignUpTableRow: TableRow {
    
    case firstName, lastName, email, password, confirmPassword
    
    var title: String {
        switch self {
        case .firstName: return LocalizableString.firstName.localizedString
        case .lastName: return LocalizableString.lastName.localizedString
        case .email: return LocalizableString.email.localizedString
        case .password: return LocalizableString.password.localizedString
        case .confirmPassword: return LocalizableString.confirmPassword.localizedString
        }
    }
}

class SignUpViewController: UIViewController {

    // MARK:- Outlets
    
    @IBOutlet weak var tableView: UITableView!
    
    /// Data Members
    fileprivate let tableRows: [SignUpTableRow] = [
        .firstName, .lastName, .email, .password, .confirmPassword
    ]
    
    lazy var signUpData = UserSignUpData()
    
    override func viewDidLoad() {
        super.viewDidLoad()

        self.navigationController?.navigationBar.isHidden = true
        setupTableView()
        // Do any additional setup after loading the view.
    }
    
    override func viewWillAppear(_ animated: Bool) {
        self.navigationController?.navigationBar.isHidden = true
    }
    
    // MARK:- UI Utils
    private func setupTableView() {
        self.tableView.dataSource = self
        self.tableView.delegate = self
    }
    
    /// This will change the root view controller to Tab Bar Controller
    static func goToHome() {

        guard let window = UIApplication.shared.keyWindow else { return }

        guard let rootViewController = window.rootViewController else { return }
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let tabBarVC = storyboard.instantiateViewController(
            withIdentifier: "SideMenu"
        )
        tabBarVC.view.frame = rootViewController.view.frame
        tabBarVC.view.layoutIfNeeded()

        UIView.transition(
            with: window,
            duration: 0.5,
            options: .transitionCrossDissolve,
            animations: { window.rootViewController = tabBarVC },
            completion: { completed in  // maybe do something here
            }
        )

    }
    
    private func navigateToVerificationScreen() {
        let verifyVC: VerificationViewController? = self.storyboard?.instantiateViewController()
        if verifyVC != nil {
            verifyVC?.userEmail = signUpData.email
            self.navigationController?.pushViewController(verifyVC!, animated: true)
        }
    }
    
    @IBAction func backBtnPressed(_ sender: UIButton) {
        self.navigationController?.popViewController(animated: true)
    }

    @IBAction func signupBtnPressed(_ sender: UIButton) {
//        SignUpViewController.goToHome()
        do {
            _ = try signUpData.isValid()
            self.requestForRegisterUser()
        } catch let err as ValidationError {
            self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
        } catch {
            
        }
    }
    
    @IBAction func termsBtnAction(_ sender: UIButton) {
        if let url = URL(string: LocalizableString.termsURL.localizedString) {
            UIApplication.shared.open(url)
        }
    }
}

extension SignUpViewController: UITableViewDataSource {
    
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
                case .password, .confirmPassword: cell.textField.isSecureTextEntry = true
                case .email: cell.textField.keyboardType = .emailAddress
                default: cell.textField.isSecureTextEntry = false
            }
            return cell
        }
        return UITableViewCell()
    }
    
}

extension SignUpViewController: UITableViewDelegate {
        
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 60
    }
    
}

//MARK: API Call's
extension SignUpViewController {
    
    //MARK: API Call for Registration
    private func requestForRegisterUser() {
        
        self.showProgress()
        AuthAPI.registeruser(userData: signUpData) { (status, error) in
            UserDefaults.standard.set(self.signUpData.email, forKey: UserDefaults.Key.login.rawValue)
            self.hideProgress()
            if status {
                self.navigateToVerificationScreen()
            } else if let error = error {
                self.presentDefaultAlertWithError(error: error, animated: true, completion: nil)
            }
        }
    }
}

extension SignUpViewController: SingleFieldTableViewCellDelegate {

    func didChangeValue(for row: TableRow, value: String) {
        guard let row = row as? SignUpTableRow
            else { return }
        switch row {
        case .firstName:
            signUpData.firstName = value
        case .lastName:
            signUpData.lastName = value
        case .email:
            signUpData.email = value
        case .password:
            signUpData.password = value
        case .confirmPassword:
            signUpData.confirmPassword = value
        }
    }

}

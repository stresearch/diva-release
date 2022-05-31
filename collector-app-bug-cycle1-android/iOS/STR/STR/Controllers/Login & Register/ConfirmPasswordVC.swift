//
//  ConfrimForgotPasswordVC.swift
//  STR
//
//  Created by Srujan on 02/04/20.
//  
//

import UIKit

enum ConfirmPasswordTableRow: TableRow {
    
    case otp, resendOtp, newPassword, confirmPassword
    
    var title: String {
        switch self {
        case .otp: return LocalizableString.otp.localizedString
        case .resendOtp: return LocalizableString.resendOtp.localizedString
        case .newPassword: return LocalizableString.newPassword.localizedString
        case .confirmPassword: return LocalizableString.confirmPassword.localizedString
        }
    }
}

class ConfirmPasswordVC: UIViewController {

    // MARK:- Outlets
    
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var labelStaticText: UILabel!
    
    fileprivate let tableRows: [ConfirmPasswordTableRow] = [
        .otp, .resendOtp, .newPassword, .confirmPassword
    ]
    var userEmail: String?
    lazy var confirmPasswordData = UserConfirmPasswordData()
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        setupTableView()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        self.navigationController?.navigationBar.isHidden = true
        labelStaticText.text = LocalizableString.otpStaticText.localizedString + "\(userEmail?.components(separatedBy: "@").last ?? "")"
    }

    // MARK:- UI Utils
    private func setupTableView() {
        self.tableView.registerCell(cell: SingleButtonTableViewCell.self)
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
    
    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */
    
    @IBAction func changePasswordAction(_ sender: UIButton) {
        do {
            _ = try confirmPasswordData.isValid()
            self.requestForConfirmPassword()
        } catch let err as ValidationError {
            self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
        } catch {
            
        }
    }
    
    @IBAction func backButtonAction(_ sender: UIButton) {
        self.navigationController?.popViewController(animated: true)
    }
}

extension ConfirmPasswordVC: UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return tableRows.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let curentRow = tableRows[indexPath.row]
        switch curentRow {
        case .confirmPassword, .newPassword, .otp: 
            if let cell = tableView.dequeueReusableCell(withIdentifier: SingleFieldTableViewCell.reuseIdentifier, for: indexPath) as? SingleFieldTableViewCell {
                cell.currentRow = curentRow
                cell.delegate = self
                cell.textField.isSecureTextEntry = true
                switch curentRow {
                case .otp:
                    cell.textField.keyboardType = .numberPad
                    if #available(iOS 12.0, *) {
                        cell.textField.textContentType = .oneTimeCode
                    } else {
                        // Fallback on earlier versions
                    }
                default: cell.textField.keyboardType = .default
                }
                return cell
            }
            return UITableViewCell()
            
            default: if let cell = tableView.dequeueReusableCell(withIdentifier: SingleButtonTableViewCell.reuseIdentifier, for: indexPath) as? SingleButtonTableViewCell {
                cell.delegate = self
                cell.titleBtn.setTitleColor(UIColor.appColor(.main), for: .normal)
                return cell
            }
            return UITableViewCell()
        }
    }
    
}

extension ConfirmPasswordVC: UITableViewDelegate {
        
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        let curentRow = tableRows[indexPath.row]
        switch curentRow {
        case .otp: return 50
        default: return 90
        }
    }

}

//Api Call
extension ConfirmPasswordVC {
    
    private func requestForConfirmPassword() {
        self.showProgress()
        AuthAPI.confirmForgotPassword(userName: userEmail ?? "", newPassword: confirmPasswordData.newPassword, otp: confirmPasswordData.otp) { (status, error) in
            self.hideProgress()
            if status {
                
                self.presentDefaultAlertWithTitle(title: LocalizableString.success.localizedString, message: LocalizableString.passwordSuccessMessage.localizedString, animated: true) {
                    self.navigationController?.popToViewController(ofClass: SignInViewController.self)
                    
                }
            } else if let error = error {
                self.presentDefaultAlertWithError(error: error, animated: true, completion: nil)
            }
        }
    }
    
}

extension ConfirmPasswordVC: SingleFieldTableViewCellDelegate {

    func didChangeValue(for row: TableRow, value: String) {
        guard let row = row as? ConfirmPasswordTableRow
            else { return }
        switch row {
        case .otp:
            confirmPasswordData.otp = value
        case .confirmPassword:
            confirmPasswordData.confirmPassword = value
        case .newPassword:
            confirmPasswordData.newPassword = value
        default: break
        }
    }

}

extension ConfirmPasswordVC: SingleButtonCellDelegate {
    
    func didTapOnButton() {
        self.showProgress()
        AuthAPI.forgotPassword(userName: userEmail ?? "") { (status, error) in
            self.hideProgress()
            if status {
                
            } else if let error = error {
                self.presentDefaultAlertWithError(error: error, animated: true, completion: nil)
            }
        }
    }
}

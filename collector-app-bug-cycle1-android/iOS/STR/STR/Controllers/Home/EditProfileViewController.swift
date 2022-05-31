//
//  EditProfileViewController.swift
//  STR
//
//  Created by Srujan on 03/02/20.
//  
//

import UIKit

class EditProfileViewController: UIViewController {

    //MARK: Outlets
    @IBOutlet weak var buttonBack: UIButton!
    @IBOutlet weak var buttonSave: UIButton!
    @IBOutlet weak var tableView: UITableView!
  
    private var changePasswordView: ChangeUserNameView?
    lazy var changePasswordData = UserChangePasswordData()
    lazy var editProfileData = UserSignUpData()
    ///
    fileprivate let tableRows: [SignUpTableRow] = [
        .firstName, .lastName, .email, .password
    ]
  
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        editProfileData.firstName = Collector.currentCollector.firstName
        editProfileData.lastName = Collector.currentCollector.lastName
        setupTableView()
    }
    
    deinit {
        Log("\(self) I'm gone ") // Keep eye on this
    }

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */
  
    // MARK:- UI Utils
    private func setupTableView() {
        self.tableView.dataSource = self
        self.tableView.delegate = self
    }
  
    private func setUpCustomView() {
        
        guard let usernameView = ChangeUserNameView.instanceFromNib()
            else { return }
        usernameView.isUsername = false
        usernameView.addTextFieldTarget()
        usernameView.delegate = self
        usernameView.viewbackground.cornerRadius = 10
        usernameView.alpha = 0
        changePasswordView = usernameView
        self.view.addSubview(usernameView)
        usernameView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            usernameView.trailingAnchor.constraint(equalTo: self.view.trailingAnchor),
            usernameView.leadingAnchor.constraint(equalTo: self.view.leadingAnchor),
            usernameView.topAnchor.constraint(equalTo: self.view.topAnchor),
            usernameView.bottomAnchor.constraint(equalTo: self.view.bottomAnchor)
        ])
        UIView.animate(withDuration: 1.0, delay: 0.1, options: [.curveEaseIn], animations: { [weak self] in
            guard let _ = self else {return}
                usernameView.alpha = 1
            }, completion: nil
        )
    }
    
    private func removeCustomView() {
        if let changePasswordView = self.changePasswordView {
            UIView.animate(withDuration: 0.5, delay: 0.1, options: [.curveEaseOut], animations: { [weak self] in
                guard let _ = self else {return}
                    changePasswordView.alpha = 0
                }, completion: { (finished) in
                    changePasswordView.removeFromSuperview()
                    self.changePasswordData = UserChangePasswordData()
            })
        }
    }
  
    //MARK: IBActions
    @IBAction func backBtnPressed(_ sender: UIButton) {
//        self.navigationController?.popViewController(animated: true)
      self.dismiss(animated: true, completion: nil)
    }

    @IBAction func saveBtnPressed(_ sender: UIButton) {
        
        do {
            _ = try editProfileData.isEditProfileValid()
            
            if editProfileData.firstName != Collector.currentCollector.firstName || editProfileData.lastName != Collector.currentCollector.lastName {
                self.requestForUpdateProfile()
            }
        } catch let err as ValidationError {
            self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
        } catch {
            
        }
    }
}

extension EditProfileViewController: ChangeUserNameViewDelegate {
    
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

extension EditProfileViewController: UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return tableRows.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
      let user = Collector.currentCollector
      let curentRow = tableRows[indexPath.row]
      switch curentRow {
      case .firstName, .lastName, .email:
        if let cell = tableView.dequeueReusableCell(withIdentifier: SingleFieldTableViewCell.reuseIdentifier, for: indexPath) as? SingleFieldTableViewCell {
            let curentRow = tableRows[indexPath.row]
            
            cell.currentRow = curentRow
            cell.delegate = self
            if curentRow == .firstName {
                cell.textField.text = user.firstName
            }
            else if curentRow == .lastName {
                cell.textField.text = user.lastName
            }
            else if curentRow == .email {
                cell.textField.text = user.email
                cell.textField.isUserInteractionEnabled = false
            }
            return cell
        }
      default:
        if let cell = tableView.dequeueReusableCell(withIdentifier: SingleFieldChangeTableViewCell.reuseIdentifier, for: indexPath) as? SingleFieldChangeTableViewCell {
            let curentRow = tableRows[indexPath.row]
            cell.textField.isUserInteractionEnabled = false
            cell.currentRow = curentRow
            cell.delegate = self
            cell.changeFieldDelegate = self
            cell.textField.text = "******"
          if curentRow == .password {
              cell.textField.isSecureTextEntry = true
          }
             return cell
        }
      }
        return UITableViewCell()
    }
    
}

extension EditProfileViewController: UITableViewDelegate {
        
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 80
    }
    
}

extension EditProfileViewController: SingleFieldTableViewCellDelegate {

    func didChangeValue(for row: TableRow, value: String) {
        guard let row = row as? SignUpTableRow
            else { return }
        switch row {
        case .firstName:
            editProfileData.firstName = value
        case .lastName:
            editProfileData.lastName = value
        default: break
        }
    }

}

extension EditProfileViewController: SingleFieldChangeTableViewCellDelegate {
  
  func didTapOnChangeButton(for row: TableRow) {
    guard let _ = row as? SignUpTableRow
               else { return }
    setUpCustomView()
  }
}

extension EditProfileViewController {
    
    private func requestForChangePassword() {
        
        self.showProgress()

        AuthAPI.changePassword(oldPassword: changePasswordData.currentPassword, newPassword: changePasswordData.newPassword) { [weak self] (status, error) in
            guard let self = self else {return}
            self.hideProgress()
            if status {
                self.presentDefaultAlertWithTitle(title: LocalizableString.success.rawValue, message: LocalizableString.passwordSuccessMessage.rawValue, animated: true) {
                    self.removeCustomView()
                }
            } else if let error = error {
                /// Show alert with error
                self.presentDefaultAlertWithError(error: error, animated: true, completion: nil)
            }
        }
    }
    
    
    private func requestForUpdateProfile() {
        
        self.showProgress()
        
        UserAPI.updateProfile(params: editProfileData) { [weak self] (status, error) in
            guard let self = self else {return}
            self.hideProgress()
            if status {
                self.presentDefaultAlertWithTitle(title: LocalizableString.success.localizedString, message: LocalizableString.profileUpdate.localizedString, animated: true) {
                    self.dismiss(animated: true, completion: nil)
                }
            } else if let err = error {
                self.tableView.reloadData()
                self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
            }
        }
    }
}

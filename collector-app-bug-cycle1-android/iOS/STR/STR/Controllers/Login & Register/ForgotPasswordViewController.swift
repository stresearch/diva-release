//
//  ForgotPasswordViewController.swift
//  STR
//
//  Created by Srujan on 01/04/20.
//  
//

import UIKit


class ForgotPasswordViewController: UIViewController {

    // MARK:- Outlets
    
    @IBOutlet weak var tableView: UITableView!
    
    fileprivate let tableRows: [SignUpTableRow] = [
        .email
    ]
    lazy var signInData = UserSignInData()
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        setupTableView()
    }
    
    override func viewWillAppear(_ animated: Bool) {
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
    
    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

    // MARK:- Actions
        
    @IBAction func sendOtpBtnPressed(_ sender: UIButton) {
        do {
            _ = try self.isEmailValid()
            self.requestForForgotPassword()
        } catch let err as ValidationError {
            self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
        } catch {

        }
    }
    
    @IBAction func backBtnPressed(_ sender: UIButton) {
        self.navigationController?.popViewController(animated: true)
    }
    
    private func navigateToConfirmPassword() {
        let confirmPassVC: ConfirmPasswordVC? = self.storyboard?.instantiateViewController()
        if confirmPassVC != nil {
            confirmPassVC?.userEmail = signInData.email
             self.navigationController?.pushViewController(confirmPassVC!, animated: true)
        }
    }
    
    // MARK: - Validations
    private func isEmailValid() throws -> Bool {
        try _ = Collector.validEmail(email: signInData.email)

        return true
    }
}

extension ForgotPasswordViewController: UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return tableRows.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        if let cell = tableView.dequeueReusableCell(withIdentifier: SingleFieldTableViewCell.reuseIdentifier, for: indexPath) as? SingleFieldTableViewCell {
            let curentRow = tableRows[indexPath.row]
            cell.currentRow = curentRow
            cell.delegate = self
            switch curentRow {
                default: cell.textField.isSecureTextEntry = false
            }
            return cell
        }
        return UITableViewCell()
    }
    
}

extension ForgotPasswordViewController: UITableViewDelegate {
        
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 90
    }

}

// Api
extension ForgotPasswordViewController {
    
    //MARK: Webservice call for ForgotPassword
    private func requestForForgotPassword() {
        self.showProgress()
        AuthAPI.forgotPassword(userName: signInData.email) { (status, error) in
            self.hideProgress()
            if status {
                self.navigateToConfirmPassword()
            } else if let error = error {
                self.presentDefaultAlertWithError(error: error, animated: true, completion: nil)
            }
        }
    }
}

extension ForgotPasswordViewController: SingleFieldTableViewCellDelegate {

    func didChangeValue(for row: TableRow, value: String) {
        guard let row = row as? SignUpTableRow
            else { return }
        switch row {
        case .email:
            signInData.email = value
        default: break
        }
    }
}

//
//  ProfileViewController.swift
//  STR
//
//  Created by SRUJAN KUMAR VEERANTI on 14/01/20.
//  
//

import UIKit
import AWSMobileClient
import SwiftyDropbox
import SafariServices

enum ProfileTableViewData: TableRow {
    case Account
    case Payment
    case Dropbox
    case WiFi
    case Help
    case EditConsent
    case SignOut
    case UploadedVideos
    case VerifiedVideos
    case AmountReceived
    case OutstandingAmount
    
    var title: String {
        return ""
    }
}

class ProfileViewController: UIViewController {

    //MARK: Outlets
    @IBOutlet weak var buttonClose: UIButton!
    @IBOutlet weak var segment: UISegmentedControl!
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var viewRevokeConsent: UIView!
    
    //MARK: Data Members
    var tableViewRowDetails: [ProfileTableViewData] = []
    var editConsent: SubjectByStrSubjectEmailQuery.Data.SubjectByStrSubjectEmail.Item?
    var isRevokeConsentButtonVisble: Bool = false
    static var isConnectingToDropBox = false
    static var isConnectingToPayPal = false
    static var payPalAuthCode = ""
    var collectorState: Collector?
    var safariVC: SFSafariViewController!
    var subjectCollectorEmail = ""
    
    // MARK:- LifeCycle
    deinit {
        Log("\(self) I'm gone ") // Keep eye on this
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()

        buttonClose.isHidden = false
        self.navigationController?.isNavigationBarHidden = true
//        segment.isUserInteractionEnabled = false
        ProfileViewController.isConnectingToDropBox = false
        ProfileViewController.isConnectingToPayPal = false
        
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        //setUpTableView()
        
        if !ProfileViewController.isConnectingToDropBox && !ProfileViewController.isConnectingToPayPal {
            requestForProfile() //requestForSubjectVerification()
        } else if ProfileViewController.isConnectingToPayPal {
            ProfileViewController.isConnectingToPayPal = false
            requestForPayPalID()
        }
//        else if ProfileViewController.isConnectingToDropBox {
//            ProfileViewController.isConnectingToDropBox = false
//            showProgress()
//            requestForUpdateProfie(true)
//        }
        addObservers()
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        removeObservers()
    }
    
    //MARK: Custom Methods
    private func setUpTableView() {
        // Enable PayPal: Add .payment at 2nd position in the array
        tableViewRowDetails = [.Account, .Dropbox, .SignOut] //[.Account, .Payment, .Dropbox, .SignOut]
        tableView.estimatedRowHeight = 200
        tableView.rowHeight = UITableView.automaticDimension
        tableView.dataSource = self
        tableView.reloadData()
    }
  
    private func addObservers() {
        NotificationCenter.default.addObserver(self, selector: #selector(updateDropBoxToken), name: .dropBox, object: nil)
    }
    
    private func removeObservers() {
        NotificationCenter.default.removeObserver(self, name: .dropBox, object: nil)
    }
    
    private func navigateToEditAccount() {
        let editProfileVC: EditProfileViewController? = self.storyboard?.instantiateViewController()
        if editProfileVC != nil {
//             self.navigationController?.pushViewController(editProfileVC!, animated: true)
            editProfileVC?.modalPresentationStyle = .fullScreen
          self.present(editProfileVC!, animated: true, completion: nil)
        }
    }
    
    private func removeSubjectFromRecentSubjects() {
        var subjects = UserDefaults.standard.value(forKey: UserDefaults.Keys.subjectEmail.rawValue) as? [String] ?? []
        if let index = subjects.firstIndex(of: Collector.currentCollector.email) {
            subjects.remove(at: index)
            UserDefaults.standard.set(subjects, forKey: UserDefaults.Keys.subjectEmail.rawValue)
        }
    }
    
    static func navigateToSplashVC() {
        let authStoryBoard = UIStoryboard(name: "Auth", bundle: nil)
        let splashVC = authStoryBoard.instantiateViewController(withIdentifier: "SplashNavigationViewController")
        let appDelegate = UIApplication.shared.delegate as! AppDelegate
        appDelegate.window?.rootViewController = splashVC
        
    }
  
    private func navigateToEditConsent() {

        let editProfileVC: EditConsentVC? = self.storyboard?.instantiateViewController()
        if editProfileVC != nil {
            //editProfileVC?.modalPresentationStyle = .fullScreen
            editProfileVC?.consentResponse = editConsent
            editProfileVC?.subjectCollectorEmail = subjectCollectorEmail
            //self.present(editProfileVC!, animated: true, completion: nil)
            self.navigationController?.pushViewController(editProfileVC!, animated: true)
        }
    }
    
    @objc private func updateDropBoxToken() {
        ProfileViewController.isConnectingToDropBox = false
        showProgress()
        requestForUpdateProfie(true)
    }
    
    @objc private func updatePayPalToken() {
        ProfileViewController.isConnectingToDropBox = false
        showProgress()
        requestForUpdateProfie(true)
    }
    
    private func connectDropBox() {
        DropboxClientsManager.authorizeFromController(UIApplication.shared,
                                                      controller: self,
                                                      openURL: { (url: URL) -> Void in
                                                        if UIApplication.shared.canOpenURL(url) {
                                                            UIApplication.shared.open(url, options: [:], completionHandler: nil)
                                                        }
                                                      })
    }
    
    private func connectPayPal() {
        
        let redirectionUrl = LocalizableString.payPalSandBoxConnectURL1.localizedString + kPayPalClientID + LocalizableString.payPalConnectURL2.localizedString + LocalizableString.payPalRedirectURI.localizedString
        
        if #available(iOS 11.0, *) {
            self.safariVC = SFSafariViewController(url: URL(string: redirectionUrl)!)
        } else {
            self.safariVC = SFSafariViewController(url: URL(string: redirectionUrl)!, entersReaderIfAvailable: true)
        }
        self.safariVC.delegate = self
        
        self.present(self.safariVC, animated: true, completion: nil)
    }
        
    //MARK: IBActions
    @IBAction func closeBtnAction(_ sender: UIButton) {
        self.navigationController?.isNavigationBarHidden = false
        self.navigationController?.popViewController(animated: true)
    }
    
    @IBAction func revokeConsentBtnAction(_ sender: UIButton) {
        UIUtilities.showAlertMessageWithTwoActionsAndHandler("Alert", errorMessage: LocalizableString.revokeConsentMessage.localizedString, errorAlertActionTitle: "NO", errorAlertActionTitle2: "YES", viewControllerUsed: self, action1: {
            
        }) {
            self.requestForRevokeConsent()
        }
    }
    
    @IBAction func segmentControlAction(_ sender: UISegmentedControl) {
        if sender.selectedSegmentIndex == 0 {
            // Enable PayPal: Add .payment at 2nd position in the array
            tableViewRowDetails = [.Account, .Dropbox, .SignOut] //[.Account, .Payment, .Dropbox, .SignOut]
        } else {
            // Enable PayPal: Add .AmountReceived & .OutstandingAmount at end of the array
            tableViewRowDetails = [.UploadedVideos, .VerifiedVideos] //[.UploadedVideos, .VerifiedVideos, .AmountReceived, .OutstandingAmount]
        }
        viewRevokeConsent.isHidden = true
        tableView.reloadData()
    }
}

extension ProfileViewController: UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return tableViewRowDetails.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let rowInfo = tableViewRowDetails[indexPath.row]
        
        switch rowInfo {
        case .Account:
            if let cell = tableView.dequeueReusableCell(withIdentifier: EditAccountTableViewCell.reuseIdentifier, for: indexPath) as? EditAccountTableViewCell {
                let user = Collector.currentCollector
                cell.delegate = self
                cell.labelAccountName.text = user.firstName + " " + (user.lastName ?? "")
                cell.labelEmail.text = user.email ?? ""
                cell.buttonEdit.setTitle("Edit Account", for: .normal)
                cell.currentRow = rowInfo
                return cell
            }
        case .Payment:
            if let cell = tableView.dequeueReusableCell(withIdentifier: EditAccountTableViewCell.reuseIdentifier, for: indexPath) as? EditAccountTableViewCell {
                cell.labelAccountName.text = "Payment"
                cell.labelEmail.text = Collector.currentCollector.payPalSetUp ? "Paypal: \(Collector.currentCollector.payPalID ?? "--")" : "Paypal: --"
                cell.buttonEdit.setTitle(Collector.currentCollector.payPalSetUp ? "Change" : "Connect", for: .normal)
                cell.currentRow = rowInfo
                cell.delegate = self
                return cell
            }
        case .Dropbox:
            if let cell = tableView.dequeueReusableCell(withIdentifier: SupportTableViewCell.reuseIdentifier, for: indexPath) as? SupportTableViewCell {
                cell.buttonTitle.setTitle("Dropbox", for: .normal)
                cell.buttonConnect.isHidden = false
                cell.buttonConnect.setTitle(Collector.currentCollector.dropBoxSetUp ? "Remove" : "Connect", for: .normal)
                cell.buttonConnect.tag = Collector.currentCollector.dropBoxSetUp ? 1 : 2
                cell.currentRow = rowInfo
                cell.delegate = self
                return cell
            }
        case .WiFi:
            if let cell = tableView.dequeueReusableCell(withIdentifier: WifiTableViewCell.reuseIdentifier, for: indexPath) as? WifiTableViewCell {
                return cell
            }
        case .Help:
            if let cell = tableView.dequeueReusableCell(withIdentifier: SupportTableViewCell.reuseIdentifier, for: indexPath) as? SupportTableViewCell {
                cell.buttonTitle.setTitle("Help & Support", for: .normal)
                cell.currentRow = rowInfo
                return cell
            }
        case .EditConsent:
            if let cell = tableView.dequeueReusableCell(withIdentifier: SupportTableViewCell.reuseIdentifier, for: indexPath) as? SupportTableViewCell {
                cell.buttonTitle.setTitle("Edit Consent", for: .normal)
                cell.delegate = self
                cell.currentRow = rowInfo
                return cell
            }
        case .SignOut:
            if let cell = tableView.dequeueReusableCell(withIdentifier: SupportTableViewCell.reuseIdentifier, for: indexPath) as? SupportTableViewCell {
                cell.buttonTitle.setTitle("Sign-Out", for: .normal)
                cell.delegate = self
                cell.currentRow = rowInfo
                return cell
            }
        case .UploadedVideos:
            if let cell = tableView.dequeueReusableCell(withIdentifier: StatsUploadVideosTableViewCell.reuseIdentifier, for: indexPath) as? StatsUploadVideosTableViewCell {
                cell.labelUploadedVideos.text = Collector.currentCollector.uploadedCount == nil
                                                ? "--"
                                                : Collector.currentCollector.uploadedCount
                return cell
            }
        case .VerifiedVideos:
            if let cell = tableView.dequeueReusableCell(withIdentifier: StatsVerifiedTableViewCell.reuseIdentifier, for: indexPath) as? StatsVerifiedTableViewCell {
                cell.labelVerifiedVideos.text = Collector.currentCollector.verifiedCount == nil
                                                ? "--"
                                                : Collector.currentCollector.verifiedCount
                cell.labelNotVerifiedVideos.text = Collector.currentCollector.notVerifiedCount == nil
                                                   ? "--"
                                                   : Collector.currentCollector.notVerifiedCount
                cell.labelConsentedVideos.text = Collector.currentCollector.consentedCount == nil
                                                 ? "--"
                                                 : Collector.currentCollector.consentedCount
                return cell
            }
        case .AmountReceived:
            if let cell = tableView.dequeueReusableCell(withIdentifier: StatsAmountTableViewCell.reuseIdentifier, for: indexPath) as? StatsAmountTableViewCell {
                cell.labelTitle.text = "Amount Received"
                cell.labelAmount.text = "$ \(Collector.currentCollector.authorized ?? "--")"
                return cell
            }
        case .OutstandingAmount:
            if let cell = tableView.dequeueReusableCell(withIdentifier: StatsAmountTableViewCell.reuseIdentifier, for: indexPath) as? StatsAmountTableViewCell {
                cell.labelTitle.text = "Outstanding Amount"
                cell.labelAmount.text = "$ \(Collector.currentCollector.outstandingAmount ?? "--")"
                return cell
            }
        }
        return UITableViewCell()
    }
}

extension ProfileViewController: EditAccountTableViewCellDelegate {
  
    func didTapOnEditAccount(for row: TableRow) {
        guard let row = row as? ProfileTableViewData
        else { return }
        switch row {
        case .Account:
            navigateToEditAccount()
        case .Payment:
            collectorState = Collector.currentCollector.copy() as? Collector
            connectPayPal()
        default:
            break
        }
    }
}

extension ProfileViewController: SupportTableViewCellDelegate {
    
    func didTapOnButtonAction(for row: TableRow) {
        
        guard let row = row as? ProfileTableViewData
        else { return }
        switch row {
        case .SignOut:
            UserDefaults.standard.reset()
            UserAPI.logout { (status, error) in
                if status {
                    ProfileViewController.navigateToSplashVC()
                } else if let err = error {
                    self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
                }
            }
        case .EditConsent:
            self.navigateToEditConsent()
        default:
            break
        }
    }
    
    func didTapOnConnectButtonAction(for row: TableRow, tag: Int) {
        guard let row = row as? ProfileTableViewData else { return }
        switch row {
        case .Dropbox:
            collectorState = Collector.currentCollector.copy() as? Collector
            if tag == 1 {
                Collector.currentCollector.dropBoxSetUp = false
                Collector.currentCollector.dropBoxToken = ""
                showProgress()
                requestForUpdateProfie(true)
            } else {
                self.connectDropBox()
            }
            
        default: break
        }
    }
}

extension ProfileViewController {
    
    private func requestForRevokeConsent() {
     
        self.showProgress()
        
        ConsentAPI.revokeConsent() { [weak self] (status, error) in
            
            guard let self = self else { return }
            
            self.hideProgress()
            if status {
                self.removeSubjectFromRecentSubjects()
                self.viewRevokeConsent.isHidden = true
                self.tableViewRowDetails = [.Account, .Dropbox, .SignOut]
                self.tableView.reloadData()
            } else if let err = error {
                self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
            }
        }
    }
    
    private func requestForProfile() {
        
        self.showProgress()
        
        UserAPI.getProfile { [weak self] (status, error) in
            guard let self = self else { return }
            self.hideProgress()
            if status {
                self.setUpTableView()
            } else if let err = error {
                self.setUpTableView()
                self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
            }
        }
    }
    
    private func requestForSubjectVerification() {
        
        self.showProgress()
        ConsentAPI.verifySubject(subjectEmail: Collector.currentCollector.email ?? "") { [weak self] (result, error) in
            guard let self = self else { return }
            self.hideProgress()
            if let err = error {
                self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
            } else if let result = result {
                
                if !(Collector.currentCollector.consentSetUp) {
                    if Collector.currentCollector.email == result.subjectEmail {
                        Collector.currentCollector.consentSetUp = true
                        self.requestForUpdateProfie(false)
                    }
                }
                self.subjectCollectorEmail = result.collectorEmail
                self.editConsent = result
                self.viewRevokeConsent.isHidden = true //self.viewRevokeConsent.isHidden = false
                self.isRevokeConsentButtonVisble = true
                self.setUpTableView()
            } else {
                self.viewRevokeConsent.isHidden = true
                self.isRevokeConsentButtonVisble = false
                self.setUpTableView()
            }
        }
    }
    
    private func requestForUpdateProfie(_ handleResponse: Bool) {
        
        let data = UserSignUpData()
        data.firstName = Collector.currentCollector.firstName
        data.lastName = Collector.currentCollector.lastName
        
        UserAPI.updateProfileToDDB(params: data) { [weak self] (status, error) in
            guard let self = self else { return }
            self.hideProgress()
            if handleResponse {
                if status {
                    self.tableView.reloadData()
                } else if let err = error {
                    self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
                    Collector.currentCollector.revert(copy: self.collectorState ?? Collector.currentCollector)
                    self.tableView.reloadData()
                }
            }
        }
    }
    
    private func requestForPayPalID() {
        
        self.showProgress()
        
        let dic = ["grant_type": "authorization_code", "code": ProfileViewController.payPalAuthCode]
        
        PayPalAPI.getPayPalID(params: dic) { [weak self] (id, error) in
            guard let self = self else { return }
            ProfileViewController.payPalAuthCode = ""
            if let payPalId = id {
                Collector.currentCollector.payPalID = payPalId
                self.requestForUpdateProfie(true)
            } else if let err = error {
                self.hideProgress()
                self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
                Collector.currentCollector.revert(copy: self.collectorState ?? Collector.currentCollector)
                self.tableView.reloadData()
            }
        }
    }
}

//MARK: SafariViewController Delegate
extension ProfileViewController: SFSafariViewControllerDelegate {
    
    func safariViewControllerDidFinish(_ controller: SFSafariViewController) {
        
        print("Safari has been loaded")
    }
    
    func safariViewController(_ controller: SFSafariViewController, didCompleteInitialLoad didLoadSuccessfully: Bool) {
        
        if !didLoadSuccessfully {
            
            //controller.dismiss(animated: true, completion: nil)
        }
    }
    
    func safariViewController(_ controller: SFSafariViewController, initialLoadDidRedirectTo URL: URL) {

    }
}

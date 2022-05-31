//
//  SetUpViewController.swift
//  STR
//
//  Created by Srujan on 08/05/20.
//  
//

import UIKit
import SwiftyDropbox
import SafariServices

enum SetUpTableRow: TableRow {
    case consent, paypal, dropbox
    
    var title: String {
        return ""
    }
}

class SetUpViewController: UIViewController {

    //MARK: Outlets
    @IBOutlet weak var buttonSave: UIButton!
    @IBOutlet weak var tableView: UITableView!
    
    //MARK: Data Members
    let tableViewRowDetails: [SetUpTableRow] = [.paypal, .dropbox] //[.consent]  //[.consent, .dropbox, .paypal] //TBD
    var collectorState: Collector?
    var safariVC: SFSafariViewController!
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        
        setUpTableView()
    }

    override func viewWillAppear(_ animated: Bool) {
        self.navigationController?.navigationBar.isHidden = true
        setUpUI()
        if ProfileViewController.isConnectingToDropBox {
            ProfileViewController.isConnectingToDropBox = false
            requestForUpdateProfie()
        } else if ProfileViewController.isConnectingToPayPal {
            ProfileViewController.isConnectingToPayPal = false
            requestForPayPalID()
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

    //MARK: Custom Methods
    
    private func setUpUI() {
        
        let collector = Collector.currentCollector
        if /*!collector.consentSetUp &&*/ !collector.payPalSetUp && !collector.dropBoxSetUp {
            buttonSave.setTitle("SKIP", for: .normal)
        } else {
            buttonSave.setTitle("CONTINUE", for: .normal)
            tableView.reloadData()
        }
    }
    
    private func setUpTableView() {
        tableView.estimatedRowHeight = 200
        tableView.rowHeight = UITableView.automaticDimension
        tableView.dataSource = self
        tableView.reloadData()
    }
    
    private func navigateToConsent() {
        let captureActivityStoryBoard = UIStoryboard(.activity)
        let consentVC: ConsentConfirmVC = captureActivityStoryBoard
            .instantiateViewController()
        ConsentResponse.instance.subjectID = Collector.currentCollector.userId
        ConsentResponse.instance.subjectEmail = Collector.currentCollector.email
        self.navigationController?.pushViewController(consentVC, animated: true)
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
    @IBAction func saveButtonAction(_ sender: UIButton) {
        SignUpViewController.goToHome()
    }
}

//MARK: TableView DataSource Methods

extension SetUpViewController: UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return tableViewRowDetails.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let rowInfo = tableViewRowDetails[indexPath.row]
        
        if let cell = tableView.dequeueReusableCell(withIdentifier: EditAccountTableViewCell.reuseIdentifier, for: indexPath) as? EditAccountTableViewCell {
            let collector = Collector.currentCollector
            switch rowInfo {
            case .consent:
                cell.labelAccountName.text = "CONSENT"
                cell.labelEmail.text = collector.consentSetUp ? "Given Consent" : "Give Consent"
                cell.buttonEdit.isHidden = collector.consentSetUp ? true : false
                cell.buttonEdit.setTitle("Record", for: .normal)
            case .paypal:
                cell.labelAccountName.text = "PAYPAL"
                cell.labelEmail.text = collector.payPalSetUp ? collector.payPalID : "Add your PayPal ID"
                cell.buttonEdit.isHidden = collector.payPalSetUp ? true : false
                cell.buttonEdit.setTitle("Add", for: .normal)
            case .dropbox:
                cell.labelAccountName.text = "DROPBOX"
                cell.labelEmail.text = collector.dropBoxSetUp ? "Connected" : "Connect your Dropbox account"
                cell.buttonEdit.isHidden = collector.dropBoxSetUp ? true : false
                cell.buttonEdit.setTitle("Connect", for: .normal)
            }
            cell.delegate = self
            cell.currentRow = rowInfo
            return cell
        }
        return UITableViewCell()
    }
    
}

extension SetUpViewController: EditAccountTableViewCellDelegate {
    
    func didTapOnEditAccount(for row: TableRow) {
        guard let row = row as? SetUpTableRow
        else { return }
        switch row {
        case .consent:
            navigateToConsent()
        case .dropbox:
            collectorState = Collector.currentCollector.copy() as? Collector
            connectDropBox()
        case .paypal:
            collectorState = Collector.currentCollector.copy() as? Collector
            connectPayPal()
        }
    }
}

//MARK: API Call
extension SetUpViewController {
    
    private func requestForUpdateProfie() {
        
        let data = UserSignUpData()
        data.firstName = Collector.currentCollector.firstName
        data.lastName = Collector.currentCollector.lastName
        
        self.showProgress()
        
        UserAPI.updateProfileToDDB(params: data) { [weak self] (status, error) in
            guard let self = self else { return }
            self.hideProgress()
            if status {
                self.tableView.reloadData()
            } else if let err = error {
                self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
                Collector.currentCollector.revert(copy: self.collectorState ?? Collector.currentCollector)
                self.tableView.reloadData()
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
                self.requestForUpdateProfie()
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
extension SetUpViewController: SFSafariViewControllerDelegate {
    
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

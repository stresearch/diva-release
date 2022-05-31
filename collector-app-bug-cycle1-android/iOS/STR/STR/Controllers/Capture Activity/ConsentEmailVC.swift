//
//  ConsentEmailVC.swift
//  STR
//
//  Created by Srujan on 02/01/20.
//  
//

import AVFoundation
import UIKit

class ConsentEmailVC: UIViewController {
    
    //MARK: IBOutlets
    @IBOutlet weak var recentSubTableView: UITableView!
    @IBOutlet weak var emailField: TextField!
    @IBOutlet weak var labelRecentSubject: UILabel!
    @IBOutlet weak var closeBtn: UIButton!
    @IBOutlet weak var moreBtn: UIButton!
    
    //MARK: Data Members

    var arrOfSubjects: [String] = []
    private var customPopUp: CustomPopUp?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        emailField.addTarget(self, action: #selector(didChangeEditing(_:)), for: .editingChanged)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        updateNavigationBar()
        setUpUI()
    }
    
    deinit {
        Log("\(self) I'm gone ") // Keep eye on this
    }
    
    // MARK:- UI Utils
    private func setupTableView() {
        self.recentSubTableView.registerCell(cell: SingleLabelTableViewCell.self)
        self.recentSubTableView.dataSource = self
        self.recentSubTableView.delegate = self
        self.recentSubTableView.tableFooterView = UIView()
    }
    
    private func updateNavigationBar() {
        self.navigationController?.navigationBar.isHidden = true
        self.title = LocalizableString.consent.localizedString.uppercased()
    }
    
    private func reloadTableView() {
        arrOfSubjects = UserDefaults.standard.value(forKey: UserDefaults.Keys.subjectEmail.rawValue) as? [String] ?? []
        if arrOfSubjects.count > 0 {
            self.labelRecentSubject.isHidden = false
            self.emailField.text = arrOfSubjects[0]
            ConsentResponse.instance.subjectEmail = arrOfSubjects[0]
            self.recentSubTableView.reloadData()
        }
    }
    
    private func setUpUI() {
        emailField.isEnabled = true
        recentSubTableView.isHidden = false
        setupTableView()
        reloadTableView()
    }
    
    private func setUpCustomView() {
        
        guard let popUp = CustomPopUp.instanceFromNib()
            else { return }
        popUp.delegate = self
        customPopUp = popUp
        popUp.viewBackground.cornerRadius = 10
        popUp.alpha = 0
        //popUp.viewHeightConstant.constant = 270
        popUp.titleTextView.text = LocalizableString.consentEmailLearnMore.localizedString
        
        if popUp.titleTextView.text?.height(withConstrainedWidth: ScreenSize.SCREEN_WIDTH - 110, font: popUp.titleTextView.font!) ?? 0 < (ScreenSize.SCREEN_HEIGHT - 200) {
            popUp.viewHeightConstant.constant = (popUp.titleTextView.text?.height(withConstrainedWidth: ScreenSize.SCREEN_WIDTH - 110, font: popUp.titleTextView.font!) ?? 0) + 90
        } else {
            popUp.viewHeightConstant.constant = ScreenSize.SCREEN_HEIGHT - 200
        }
        
        self.view.addSubview(popUp)
        popUp.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            popUp.trailingAnchor.constraint(equalTo: self.view.trailingAnchor),
            popUp.leadingAnchor.constraint(equalTo: self.view.leadingAnchor),
            popUp.topAnchor.constraint(equalTo: self.view.topAnchor),
            popUp.bottomAnchor.constraint(equalTo: self.view.bottomAnchor)
        ])
        UIView.animate(withDuration: 0.5, delay: 0.1, options: [.curveEaseIn], animations: { [weak self] in
            guard let _ = self else {return}
                popUp.alpha = 1
            }, completion: nil
        )
    }
    
    private func removeCustomView() {
        if let popUp = self.customPopUp {
            UIView.animate(withDuration: 0.5, delay: 0.1, options: [.curveEaseOut], animations: { [weak self] in
                guard let _ = self else {return}
                    popUp.alpha = 0
                }, completion: { (finished) in
                    popUp.removeFromSuperview()
            })
        }
    }
    
    // MARK:- Actions
    @IBAction func closeButtonAction(_ sender: UIButton) {
        self.showAlertWithDecision(title: "Close", message: kExitCollection, successHandler: { [weak self] (_) in
            self?.navigationController?.popToRootViewController(animated: true)
            }, completion: nil)
    }
    
    @IBAction func moreButtonAction(_ sender: UIButton) {
        self.setUpCustomView()
    }
    
    @IBAction func backBtnPressed(_ sender: UIButton) {
        self.navigationController?.popViewController(animated: true)
    }
    
    @IBAction func confirmBtnPressed(_ sender: UIButton) {
        // show consent confirmation vc
        
        guard let email = ConsentResponse.instance.subjectEmail, !email.isEmpty else {
            let error = ValidationError(message: LocalizableString.emailMandatory.localizedString)
            self.presentDefaultAlertWithError(error: error, animated: true, completion: nil)
            return
        }

        guard Validator.valid(value: email.lowercased(), inPattern: .email) else {
            let error = ValidationError(message: LocalizableString.emailInvalid.localizedString)
            self.presentDefaultAlertWithError(error: error, animated: true, completion: nil)
            return
        }
        
        checkSubjectEmailExist()
    }
    
    @objc private func didChangeEditing(_ sender: UITextField) {
        if let value = sender.text {
            ConsentResponse.instance.subjectEmail = value
        }
    }
    
    private func navigateToConsentConfirmVC() {
        if let consentConfirmVC: ConsentConfirmVC = self.storyboard?
            .instantiateViewController() {
            self.navigationController?
                .pushViewController(consentConfirmVC, animated: true)
        }
    }
    
    private func navigateToTrainingVideo() {
        let videoURL = URL(fileURLWithPath: ProjectService.instance.trainingVideoUrl)
        let asset = AVAsset(url: videoURL)
        let videoTrack = asset.tracks(withMediaType: AVMediaType.video)[0]
        if videoTrack.naturalSize.width > videoTrack.naturalSize.height {
            VideoVariables.deviceOrientationRawValue = Utilities.getOrientationInInt(value: "landscapeLeft")
        } else {
            VideoVariables.deviceOrientationRawValue = Utilities.getOrientationInInt(value: "portrait")
        }
        
        let lastViewController = self.navigationController?.viewControllers.last
        
        if !(lastViewController?.isKind(of: ConsentVideoPreviewVC.self))! {
            
            self.view.isUserInteractionEnabled = true
            self.hideProgress()
            let previewVC: ConsentVideoPreviewVC = (self.storyboard?.instantiateViewController())!
            previewVC.fileUrl = ""
            previewVC.videoURL = videoURL
            previewVC.previewFlow = .DemoVideo
            previewVC.serverTraining = true
            self.navigationController?.pushViewController(previewVC, animated: true)
        }
    }
    
    private func navigateToRecordVideoVC() {
        // navigate to activity record
        let recordVideoVC: RecordVideoVC = (self.storyboard?.instantiateViewController())!
        // update the navigation root view controller to record video vc
        // remove all the other vc from memory and re instantiate the tab bar vc
        // update the navgation bar
        self.navigationController?.pushViewController(recordVideoVC, animated: true)
    }
    
    static func addRecentSubjectToLocalCache() {
        var subjects = UserDefaults.standard.value(forKey: UserDefaults.Keys.subjectEmail.rawValue) as? [String] ?? []
        if subjects.count >= 5 {
            subjects.remove(at: 0)
        }
        if let index = subjects.firstIndex(of: ConsentResponse.instance.subjectEmail ?? "") {
            subjects.remove(at: index)
        }
        subjects.insert(ConsentResponse.instance.subjectEmail ?? "", at: 0)
        UserDefaults.standard.set(subjects, forKey: UserDefaults.Keys.subjectEmail.rawValue)
    }
}
extension ConsentEmailVC: UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return arrOfSubjects.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        guard let cell = tableView.dequeueReusableCell(withIdentifier: SingleLabelTableViewCell.reuseIdentifier) as? SingleLabelTableViewCell else {
            return UITableViewCell()
        }
        let emailValue = arrOfSubjects[indexPath.row] == (ConsentResponse.instance.subjectEmail ?? "")
        cell.titleLbl.textColor = emailValue ? UIColor.black : UIColor.darkGray
        cell.titleLbl.text = arrOfSubjects[indexPath.row]
        
        return cell
    }
    
}

extension ConsentEmailVC: UITableViewDelegate {
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 40
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        
        ConsentResponse.instance.subjectEmail = arrOfSubjects[indexPath.row]
        
        self.emailField.text = ConsentResponse.instance.subjectEmail
        self.recentSubTableView.reloadData()
    }
}

extension ConsentEmailVC: CustomPopUpDelegate {
    
    func didTapOnOkBtn() {
        self.removeCustomView()
    }
}

//MARK: Api Call
extension ConsentEmailVC {
    
    private func checkSubjectEmailExist() {
        
        self.showProgress()
        
        let programName = ProjectService.instance.currentCollection?.programName ?? ""
        ConsentAPI.verifySubject(subjectEmail: programName + "_" + (ConsentResponse.instance.subjectEmail ?? "")) { [weak self] (result, error) in
            
            guard let self = self else { return }
            self.hideProgress()
            
            if let subject = result {
                if subject.status == "inActive" {
                    self.presentDefaultAlertWithTitle(title: nil, message: LocalizableString.subjectInActive.localizedString, animated: true, completion: nil)
                } else {
                    ConsentResponse.instance.subjectID = result?.uuid ?? Collector.currentCollector.userId
                    ConsentEmailVC.addRecentSubjectToLocalCache()
                    
                    if (ProjectService.instance.currentCollection?.isTrainingVideoEnabled ?? false) {
                        
                        if ProjectService.instance.trainingVideoUrl != "" {
                            self.navigateToTrainingVideo()
                        } else {
                            
                            UIUtilities.showAlertMessageWithActionHandler(kTrainingVideoDataMissing, message: "", buttonTitle: "OK", viewControllerUsed: self) {
                                    self.navigateToRecordVideoVC()
                            }
                        }
                    } else {
                        self.navigateToRecordVideoVC()
                    }
                }
            } else if let err = error {
                self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
            } else {
                UIUtilities.showAlertMessageWithTwoActionsAndHandler(LocalizableString.alert.localizedString,
                                                                     errorMessage: LocalizableString.consentTypoAlert.localizedString,
                                                                     errorAlertActionTitle: LocalizableString.cancel.localizedString,
                                                                     errorAlertActionTitle2: LocalizableString.ok.localizedString,
                                                                     viewControllerUsed: self, action1: {
                    
                                                                    }) {
                                                                        self.checkCollectorEmailExist()
                                                                    }
            }
        }
    }
    
    private func checkCollectorEmailExist() {
        
        self.showProgress()
        
        UserAPI.verifyCollectorEmail(collectorEmail: ConsentResponse.instance.subjectEmail ?? "") { [weak self] (result, error) in
            
            guard let self = self else { return }
            self.hideProgress()
            
            if let user = result {
                ConsentResponse.instance.subjectID = user.collectorId
                self.navigateToConsentConfirmVC()
            } else if let err = error {
                self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
            } else {
                ConsentResponse.instance.subjectID = UUID().uuidString
                self.navigateToConsentConfirmVC()
            }
        }
        
    }
}

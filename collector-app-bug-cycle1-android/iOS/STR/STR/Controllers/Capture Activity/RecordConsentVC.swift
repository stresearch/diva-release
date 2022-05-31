//
//  RecordConsentVC.swift
//  STR
//
//  Created by Srujan on 07/01/20.
//  
//

import UIKit

class RecordConsentVC: CameraController {
    
    @IBOutlet weak var recordView: RecordView!
    @IBOutlet weak var backBtn: UIButton!
    @IBOutlet weak var backBtnConstraintTop: NSLayoutConstraint!
    @IBOutlet weak var labelOverLayText: UILabel!
    
    var timer: Timer?
    var timerSeconds = kConsentVideoMaxDuration
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        setupCamera()
        setupRecordButton()
        self.navigationController?.navigationBar.isHidden = true
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if UIDevice.current.hasNotch {
            if ScreenSize.SCREEN_HEIGHT == 896 {
                backBtnConstraintTop.constant = backBtnConstraintTop.constant + 8
            }
        }
        let overLayText = ProjectService.instance.currentCollection?.consentOverlayText
        labelOverLayText.text = overLayText == "" || overLayText == nil
            ? LocalizableString.consentOverLayText.localizedString
            : ProjectService.instance.currentCollection?.consentOverlayText
    }
    
    override func viewDidAppear(_ animated: Bool) {
        NotificationCenter.default.addObserver(self, selector: #selector(willEnterForeground), name: UIApplication.willEnterForegroundNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(didEnterBackground), name: UIApplication.didEnterBackgroundNotification, object: nil)
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        
        NotificationCenter.default.removeObserver(self, name: UIApplication.willEnterForegroundNotification, object: nil)
    }
    
    // MARK: Private Methods
    @objc func willEnterForeground() {
        DispatchQueue.main.after(TimeInterval.zero + 0.08) {
            self.hideProgress()
        }
    }
    
    @objc func didEnterBackground() {
        deinitialisingTasks()
    }
    
    private func deinitialisingTasks() {
        //To discard Video if went to background
        if isVideoRecording {
            self.discardVideoRecording()
            recordView.updateButtonView(isVideoRecording: isVideoRecording)
            recordView.recordButton.delegate = self
        }
        timer?.invalidate()
        timerSeconds = kConsentVideoMaxDuration
        self.hideProgress()
    }
    
    // MARK:- UI Utils
    
    private func setupRecordButton() {
        recordView.updateButtonView(isVideoRecording: isVideoRecording)
        recordView.recordButton.delegate = self
    }
    
    fileprivate func setupCamera() {
        cameraDelegate = self
        shouldUseDeviceOrientation = true
        allowAutoRotate = false
        audioEnabled = true
        isVideoRecordingWithMask = false
    }
    
    private func showConsentVideoPreviewVC(with url: URL) {
        let path = LocalizableString.consentS3FolderStructure.localizedString
        let userEmail = Collector.currentCollector.email ?? ""
        let subjectEmail = (ProjectService.instance.currentCollection?.programName ?? "") + "_" + (ConsentResponse.instance.subjectEmail ?? "")
        var fileName = ""
        if ConsentResponse.instance.isEditConsent {
            let currentUTCDate = DateHelper.dateUTCCurrentDate(dateformateString: "yyyy-MM-dd'T'HH:mm:ssZ")
            fileName = path + userEmail + "/" + subjectEmail + "/" + "consent_video" + "_" + currentUTCDate
        } else {
            fileName = path + userEmail + "/" + subjectEmail + "/" + "consent_video"
        }
        
        ConsentResponse.instance.consentS3UrlPath = fileName + ".mp4"
        let previewVC: ConsentVideoPreviewVC = (self.storyboard?.instantiateViewController())!
        previewVC.videoURL = url
        previewVC.previewFlow = .ConsentVideo
        previewVC.fileName = fileName
        self.navigationController?.pushViewController(previewVC, animated: false)
    }
    
    // MARK:- Actions
    
    @IBAction func backBtnPRessed(_ sender: UIButton) {
        if ConsentResponse.instance.isEditConsent {
            UIUtilities.showAlertMessageWithTwoActionsAndHandler("Close", errorMessage: kExitConsent, errorAlertActionTitle: LocalizableString.ok.localizedString, errorAlertActionTitle2: LocalizableString.cancel.localizedString, viewControllerUsed: self, action1: {
                
                self.navigationController?.popToViewController(ofClass: EditConsentVC.self)
            }) {
            }
        }
        else {
            
            self.showAlertWithDecision(title: "Close", message: kExitCollection, successHandler: { [weak self] (_) in
                self?.navigationController?.popToRootViewController(animated: true)
                }, completion: nil)
        }
    }
    
}
//MARK:- Actions

extension RecordConsentVC : CameraControllerDelegate {
    
    func cameraController(_ cameraController: CameraController, didChangeZoomLevel zoom: CGFloat, zView: UIView, direction: String, gesture: UIPinchGestureRecognizer, location: CGPoint) {
        
    }
    
    func cameraController(_ cameraController: CameraController, didFinishProcessVideoAt url: URL) {
        showConsentVideoPreviewVC(with: url)
    }
    
    func cameraController(_ cameraController: CameraController, didFinishRecordingVideo camera: CameraController.CameraSelection) {
        self.timer?.invalidate()
        self.timerSeconds = kConsentVideoMaxDuration
        
        UIView.animate(withDuration: 0.2) { [weak self] in
            guard let self = self else {return}
            self.recordView?.updateButtonView(isVideoRecording: self.isVideoRecording)
            self.backBtn.isHidden = false
            
        }
    }
    
    func cameraController(_ cameraController: CameraController, didBeginRecordingVideo camera: CameraController.CameraSelection) {
        UIView.animate(withDuration: 0.2) { [weak self] in
            guard let self = self else {return}
            self.recordView?.updateButtonView(isVideoRecording: self.isVideoRecording)
            self.backBtn.isHidden = true
            
            self.startTimer()
        }
    }
    
    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true, block: { (timer) in
            self.timerSeconds -= 1
            let value = "\(kConsentVideoMaxDuration - self.timerSeconds)"
            self.recordView?.timeLbl.text = "00:\(value.count == 1 ? ("0" + value) : value)"
            if self.timerSeconds == 0 {
                DispatchQueue.main.after(TimeInterval.zero + 0.1) {
                    self.stopVideoRecording()
                    timer.invalidate()
                }
            }
        })
    }
}

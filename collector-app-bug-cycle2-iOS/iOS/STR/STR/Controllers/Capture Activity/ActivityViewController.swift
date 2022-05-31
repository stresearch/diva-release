//
//  SubActivityListVC.swift
//  STR
//
//  Created by Srujan on 02/01/20.
//  
//

import AVFoundation
import UIKit

class ActivityViewController: UIViewController {
    
    // MARK:- Outlets
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var activityTitleLbl: UILabel!
    @IBOutlet weak var textView: UITextView!
    
    //MARK: Data Members
    var arrTrainingVideos: [String] = []
    var firstTrainingVideoUrl = ""
    var bffFirstTrainingVideoUrl = ""
    var secondTrainingVideoUrl = ""
    
    override func viewDidLoad()  {
        super.viewDidLoad()
        
        //setupTableView()
        updateNavigationBar()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        ConsentResponse.instance.subjectID = ""
        convertArrayToString()
        updateNavigationBar()
        updateText()
    }
    
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        self.navigationController?.setNavigationBarHidden(true, animated: false)
    }
    
    deinit {
        Log("\(self) I'm gone ") // Keep eye on this
    }
    
    func videoSelector() {
        let videos = arrTrainingVideos
        if let video = videos.randomElement() {
	    firstTrainingVideoUrl = video.replacingOccurrences(of: "https://visym-public-data140008-visymcprod.s3.amazonaws.com/", with: "").removingWhitespaces()	    
        }
        if arrTrainingVideos.count > 1 && secondTrainingVideoUrl.isEmpty && !firstTrainingVideoUrl.isEmpty {
            
            let video = videos[0]
            secondTrainingVideoUrl = video.replacingOccurrences(of: "https://visym-public-data140008-visymcprod.s3.amazonaws.com/", with: "").removingWhitespaces()	    
            if secondTrainingVideoUrl == firstTrainingVideoUrl {
                let video1 = videos[1]
		secondTrainingVideoUrl = video1.replacingOccurrences(of: "https://visym-public-data140008-visymcprod.s3.amazonaws.com/", with: "").removingWhitespaces()		
            }
            
        }
    }
    
    private func convertArrayToString() {
        arrTrainingVideos = ProjectService.instance.currentCollection?.trainingVideos?
            .replacingOccurrences(of: "[", with: "")
            .replacingOccurrences(of: "]", with: "")
            .components(separatedBy: ",") ?? []
        
        if arrTrainingVideos.count > 0 {
            videoSelector()
        }
    }
    
    // MARK:- UI Utils
    private func setupTableView() {
        self.tableView.registerCell(cell: SingleLabelTableViewCell.self)
        self.tableView.dataSource = self
        self.tableView.estimatedRowHeight = 50
        self.tableView.rowHeight = UITableView.automaticDimension
        self.tableView.alwaysBounceVertical = false
    }
    
    
    private func updateNavigationBar() {
        self.navigationController?.navigationBar.isHidden = true
        if ProjectService.instance.isPracticeProject ?? false {
            self.activityTitleLbl.text = "Practice"
        } else {
            self.activityTitleLbl.text = ProjectService.instance.currentCollection?.collectionName ?? ""
        }
    }
    
    private func updateText() {
        
        let str1 = "<span style=\"font-family: '-apple-system'; font-size:17px;font-style:Medium;\">\(ProjectService.instance.currentCollection?.collectionDescription?.replacingOccurrences(of: "\n", with: "<br>") ?? "")</span>"
        let htmlData = NSString(string: str1).data(using: String.Encoding.utf8.rawValue)
        let options = [NSAttributedString.DocumentReadingOptionKey.documentType: NSAttributedString.DocumentType.html]
        let attributedString = try! NSAttributedString(data: htmlData!,
        options: options,
        documentAttributes: nil)
        textView.attributedText = attributedString
    }
    
    // MARK:- Actions
    
    @IBAction func backBtnPressed(_ sender: UIButton) {
        self.navigationController?.popViewController(animated: true)
    }
    
    @IBAction func nextBtnPressed(_ sender: UIButton) {
        
        if ProjectService.instance.isPracticeProject ?? false {
            let recordVideoVC: RecordVideoVC = (self.storyboard?.instantiateViewController())!
            // update the navigation root view controller to record video vc
            // remove all the other vc from memory and re instantiate the tab bar vc
            // update the navgation bar
            self.navigationController?.pushViewController(recordVideoVC, animated: true)
        } else {
            ProjectService.instance.trainingVideoUrl = ""
            ProjectService.instance.trainingVideoTextIndex = 0
            if (ProjectService.instance.currentCollection?.isConsentRequired ?? false) && (ProjectService.instance.currentCollection?.isTrainingVideoEnabled ?? false) {
                downloadTrainingVideo()
            } else if (ProjectService.instance.currentCollection?.isConsentRequired ?? false) {
                navigateToConsentEmail()
            } else if (ProjectService.instance.currentCollection?.isTrainingVideoEnabled ?? false) {
                downloadTrainingVideo()
            } else {
                navigateToVideoRecording()
            }
        }
    }
}

extension ActivityViewController: UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if ProjectService.instance.isPracticeProject ?? false {
            return 4
        }
        return  1
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        guard let cell = tableView.dequeueReusableCell(withIdentifier: SingleLabelTableViewCell.reuseIdentifier) as? SingleLabelTableViewCell else {
            return UITableViewCell()
        }
        
        if ProjectService.instance.isPracticeProject ?? false {
            cell.titleLbl.text = "Activity \(indexPath.row + 1)"
        }
        else {
            
            do {
                cell.titleLbl.attributedText = try NSMutableAttributedString(HTMLString: ProjectService.instance.currentCollection?.collectionDescription?.replacingOccurrences(of: "\n", with: "<br>") ?? "", font: UIFont.systemFont(ofSize: 17, weight: .medium))
            } catch  {
                cell.titleLbl.text = ""
            }             //ProjectService.instance.currentCollection?.collectionDescription?.replacingOccurrences(of: "\n", with: "<br>").htmlToString
        }
        return cell
    }
    
}

//MARK: Demo Video Training
extension ActivityViewController {
    
    private func downloadVideoFile(trainingVideoUrl: String) {
        
        Utilities.removeFileFromCache(fileName: "Training1.mp4")
        Utilities.removeFileFromCache(fileName: "Training1.json")
        
        print("1---\(Utilities.getFileUrlStringFromCache(fileName: "Training1.mp4"))")
            
        let url = Utilities.getFileUrlStringFromCache(fileName: "Training1.mp4")
        AWSS3Manager.shared.downloadFile(fileUrlString: url, fileName: trainingVideoUrl, bucketName: LocalizableString.bucketPublicName.localizedString) { (status, error) in
            
            if let statusVal = status, statusVal == true && (error == nil && error?.localizedDescription.isEmpty ?? true ) {
                print("True---\(statusVal)")
                self.loadServerDemoVideo()
            }
            else {
                if !self.firstTrainingVideoUrl.isEmpty && !self.secondTrainingVideoUrl.isEmpty {
                    self.bffFirstTrainingVideoUrl = self.firstTrainingVideoUrl
                    self.firstTrainingVideoUrl = ""
                    if let index = self.arrTrainingVideos.firstIndex(where: {$0.removingWhitespaces() == "https://visym-public-data140008-visymcprod.s3.amazonaws.com/" + self.secondTrainingVideoUrl}) {
                        ProjectService.instance.trainingVideoTextIndex = index
                    }
                    self.downloadVideoFile(trainingVideoUrl: self.secondTrainingVideoUrl)
                    
                }
                else if trainingVideoUrl == self.bffFirstTrainingVideoUrl {
                    
                }
                else {
                    self.handleIfNoTrainingVideo()
                }
            }
        }
    }
    
    func loadServerDemoVideo() {
        let videoURL = URL(fileURLWithPath: Utilities.getFileUrlStringFromCache(fileName: "Training1.mp4"))
        ProjectService.instance.trainingVideoUrl = Utilities.getFileUrlStringFromCache(fileName: "Training1.mp4")
        if (ProjectService.instance.currentCollection?.isConsentRequired ?? false) {
            self.hideProgress()
            navigateToConsentEmail()
        } else if (ProjectService.instance.currentCollection?.isTrainingVideoEnabled ?? false) {
            self.loadNextTrainingScreen(fileUrl: "", videoURL: videoURL, serverTraining: true)
        }
    }
    
    func handleIfNoTrainingVideo() {//Load Project Video
            //        valVideoFromLocalURL()
            Utilities.removeFileFromCache(fileName: "Training1.mp4")
            Utilities.removeFileFromCache(fileName: "Training1.json")
            self.hideProgress()
        ProjectService.instance.trainingVideoTextIndex = 0
        if (ProjectService.instance.currentCollection?.isConsentRequired ?? false) {
            navigateToConsentEmail()
        } else {
            UIUtilities.showAlertMessageWithActionHandler(kTrainingVideoDataMissing, message: "", buttonTitle: "OK", viewControllerUsed: self) {
                    self.view.isUserInteractionEnabled = true
                    self.navigateToVideoRecording()
            }
        }
    }
    
    private func valVideoFromLocalURL(){
        let videoURL = URL(fileURLWithPath: Utilities.getFilePathFromLocal(fileName: "PreviewVideo", type: "mov")!)
        
        VideoVariables.deviceOrientationRawValue = Utilities.getOrientationInInt(value: "portrait")
        loadNextTrainingScreen(fileUrl: "",videoURL: videoURL, serverTraining: true)
    }
    
    private func validateTrainingVideoAndIDs() -> Bool {
        if !(firstTrainingVideoUrl.isEmpty) && !(ProjectService.instance.isPracticeProject ?? false) {
            return true
        }
        return false
    }
    
    func loadNextTrainingScreen(fileUrl: String, videoURL: URL,serverTraining: Bool) {
        
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
            previewVC.fileUrl = fileUrl
            previewVC.videoURL = videoURL
            previewVC.previewFlow = .DemoVideo
            previewVC.serverTraining = serverTraining
            self.navigationController?.pushViewController(previewVC, animated: true)
        }
    }
    
    private func navigateToConsentEmail() {
        
        let lastViewController = self.navigationController?.viewControllers.last
        
        if !(lastViewController?.isKind(of: ConsentEmailVC.self))! {
            
            // navigate to Consent Email VC
            let consentEmailVC: ConsentEmailVC = (self.storyboard?.instantiateViewController())!
            // update the navigation root view controller to record video vc
            // remove all the other vc from memory and re instantiate the tab bar vc
            // update the navgation bar
            self.navigationController?.pushViewController(consentEmailVC, animated: true)
        }
    }
    
    private func downloadTrainingVideo() {
        self.showProgress()
        self.view.isUserInteractionEnabled = false
        if self.validateTrainingVideoAndIDs() {
            if let index = arrTrainingVideos.firstIndex(where: {$0.removingWhitespaces() == "https://visym-public-data140008-visymcprod.s3.amazonaws.com/" + firstTrainingVideoUrl}) {
                ProjectService.instance.trainingVideoTextIndex = index
            }
            self.downloadVideoFile(trainingVideoUrl: firstTrainingVideoUrl)
        } else {
            self.handleIfNoTrainingVideo()
        }
    }
    
    private func navigateToVideoRecording() {
        // navigate to activity record
        let recordVideoVC: RecordVideoVC = (self.storyboard?.instantiateViewController())!
        // update the navigation root view controller to record video vc
        // remove all the other vc from memory and re instantiate the tab bar vc
        // update the navgation bar
        self.navigationController?.pushViewController(recordVideoVC, animated: true)
    }
}



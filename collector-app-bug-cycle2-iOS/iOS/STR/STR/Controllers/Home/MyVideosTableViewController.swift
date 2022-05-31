//
//  MyVideosTableViewController.swift
//  STR
//
//  Created by Srujan on 06/01/20.
//  
//

import UIKit
import MessageUI

class MyVideosTableViewController: UITableViewController {
    
    //MARK: Data Members
    lazy var myVideos: [StrVideoByUploaedDateQQuery.Data.StrVideoByUploaedDateQ.Item]? = []
    
    lazy var videoDevInput = UpdateStrVideosInput(id: "", videoId: "", uploadedDate: "", queryAttribute: "0")
    
    var collectionVideoUrl = ""
    var collectionVideoJsonUrl = ""
    var videoToken: String = ""
    var videoName: String = ""
    var jsonName: String = ""
    
    var arrDropDownObject: [String] = []
    var arrDropDownActivity: [String] = []
    var programID = ""
    var projectName = ""
    var collectionDescription = ""
    var isEditVideo = false
    var isShareVideo = false
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Uncomment the following line to preserve selection between presentations
        // self.clearsSelectionOnViewWillAppear = false
        
        // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
        // self.navigationItem.rightBarButtonItem = self.editButtonItem
        
        setupTableView()
        
        self.refreshControl?.isEnabled = true
        self.refreshControl?.addTarget(self, action: #selector(refresh), for: UIControl.Event.valueChanged)
        self.tabBarController?.tabBar.isUserInteractionEnabled = true
    }
    
    override func viewWillAppear(_ animated: Bool) {
        MenuController.panGestureRecognizer?.isEnabled = true
        self.navigationController?.isNavigationBarHidden = false
        setupNavBar()
        if myVideos?.count ?? 0 == 0 {
            self.requestForCollections(token: "")
        }
        for tabBarItem in (self.tabBarController?.tabBar.items)! {
            tabBarItem.title = ""
        }
    }
    
    // MARK:- UI Utils
    
    private func setupTableView() {
        self.tableView.estimatedRowHeight = 300
        self.tableView.rowHeight = UITableView.automaticDimension
        self.tableView.registerCell(cell: VideoTableViewCell.self)
    }
    
    private func setupNavBar() {
        self.navigationItem.leftBarButtonItem = leftItem()
    }
    
    func leftItem() -> UIBarButtonItem  {
        let button = UIButton.init(type: .custom)
        button.addTarget(self, action: #selector(sideMenuAction(_:)), for: .touchUpInside)
        let imageView = UIImageView(frame: CGRect(x: 0, y: 5, width: 15, height: 15))
        imageView.image = UIImage(named: "side_Menu")
        
        let shadow = NSShadow()
        shadow.shadowColor = UIColor(red: 1.0, green: 1.0, blue: 1.0, alpha: 1.0)
        shadow.shadowOffset = CGSize(width: 0, height: 1)
        let color : UIColor = UIColor.darkText
        let titleFont : UIFont = UIFont.systemFont(ofSize: 18, weight: .bold)
        
        let attributes = [
            NSAttributedString.Key.foregroundColor : color,
            NSAttributedString.Key.shadow : shadow,
            NSAttributedString.Key.font : titleFont
        ]
        
        let label = UILabel(frame: CGRect(x: 26, y: 0, width: 200, height: 30))
        label.attributedText = NSAttributedString(string: "", attributes: attributes)
        
        let buttonView = UIView(frame: CGRect(x: 0, y: 0, width: 25, height: 25))
        button.frame = buttonView.frame
        buttonView.addSubview(button)
        buttonView.addSubview(imageView)
        buttonView.addSubview(label)
        let barButton = UIBarButtonItem.init(customView: buttonView)
        
        self.title = "VIDEOS" //Set Title
        self.navigationController?.navigationBar.titleTextAttributes = attributes
        
       // self.navigationController?.navigationBar.setBackgroundImage(UIImage(), for: UIBarMetrics.default)
       // self.navigationController?.navigationBar.shadowImage = UIImage()
        self.navigationController?.navigationBar.shouldRemoveShadow(true)
        
        return barButton
    }
    
    @objc func sideMenuAction(_ sender:AnyObject) {
        sideMenuController?.revealMenu()
    }
    
    private func footer() {
        if myVideos?.count ?? 0 == 0  && videoToken == ""{
            guard let usernameView = EmptyVideoView.instanceFromNib()
                else { return }
            usernameView.delegate = self
            self.tableView.tableFooterView = usernameView
        }
        else {
            self.tableView.tableFooterView = UIView()
        }
    }
    
    // MARK: - Table view data source
    override func numberOfSections(in tableView: UITableView) -> Int {
        // #warning Incomplete implementation, return the number of sections
        return 1
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        // #warning Incomplete implementation, return the number of rows
        return myVideos?.count ?? 0
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        if let cell = tableView.dequeueReusableCell(withIdentifier: VideoTableViewCell.reuseIdentifier, for: indexPath) as? VideoTableViewCell, myVideos?.count ?? 0 > 0 {
            let video = myVideos?[indexPath.row]
            //print("video?.thumbnailSmall---\(video?.createdDate)")
            if (video?.thumbnailSmall ?? "").isEmpty {
                cell.noTumbnailView.isHidden = false
            }
            else {
                cell.noTumbnailView.isHidden = true
                let tumbnailImageURLString = "\(video?.thumbnailSmall ?? "")"
                cell.previewImageView.setImageUsingUrl(tumbnailImageURLString)
            }
            
            cell.idLbl.text = video?.collectionName ?? ""
            
            if let status = video?.videoState {
                cell.statusLbl.text = status + "        "
            } else {
                cell.statusLbl.text = ""
            }
            
            //            cell.activityTypelbl.text = "\(removeExtrasActivitiesList(text: video?.activitiesList ?? ""))"
            cell.createdDatelbl.text = DateHelper.getVideoCollectedDate(date: video?.uploadedDate ?? "")
            /*
            let percentage = video?.rating ?? ""
            cell.likePercentLbl.text = percentage + "%"
            
            if percentage.isEmpty {*/
                cell.buttonLikePercentHt.constant = 0
                cell.buttonLikePercentBt.constant = 5
                cell.buttonLikePercent.setImage(UIImage(), for: .normal)
                cell.likePercentLbl.text = ""
            //}
            
            var duration = Double(video?.duration ?? "0") ?? 0
            duration = duration.truncate(places: 0)
            let durationVal = DateHelper.convertformat2(second: duration)
            
            cell.videoDurationLbl.text = "\(durationVal)    "
            cell.videoInfo = video
            cell.delegate = self
            
            return cell
        }
        return UITableViewCell()
    }
    
    func removeExtrasActivitiesList(text: String) -> String {
        let val1 = text.replacingOccurrences(of: "[", with: "")
        let val2 = val1.replacingOccurrences(of: "]", with: "")
        let val3 = val2.replacingOccurrences(of: " ", with: "")
        let val4 = val3.replacingOccurrences(of: "\"", with: "")
        
        let arr1 = val4.components(separatedBy: ",")
        var str = ""
        
        for i in 0 ... (arr1.count - 1) {
            str.append("\(arr1[i])")
            if i != arr1.count - 1 {
                str.append(",")
            }
        }
        
        return "\(str)"
    }
    
    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }
    
    /*
     override func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
     guard let videoCell = (cell as? VideoTableViewCell) else { return }
     let visibleCells = tableView.visibleCells
     let minIndex = visibleCells.startIndex
     if tableView.visibleCells.firstIndex(of: cell) == minIndex {
     
     }
     videoCell.videoPreviewView.player?.play()
     }
     
     override func tableView(_ tableView: UITableView, didEndDisplaying cell: UITableViewCell, forRowAt indexPath: IndexPath) {
     guard let videoCell = cell as? VideoTableViewCell else { return }
     videoCell.videoPreviewView.player?.pause()
     videoCell.videoPreviewView.player = nil
     }
     */
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        if let videos = myVideos, videos.count > 0{
            self.isEditVideo = false
            self.requestForObjectAndActivityList(videoInfo: videos[indexPath.row])
        }
    }
    
    override func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        
        if indexPath.row == tableView.numberOfRows(inSection: 0) - 1 { // Last Row Reached
            if videoToken != "" {
                let spinner = UIActivityIndicatorView(style: .gray)
                spinner.startAnimating()
                spinner.frame = CGRect(x: CGFloat(0), y: CGFloat(0), width: tableView.bounds.width, height: CGFloat(44))

                self.tableView.tableFooterView = spinner
                self.tableView.tableFooterView?.isHidden = false
                self.requestForCollections(token: self.videoToken)
            }
        }
    }
    
    //MARK: IBActions
    @objc func refresh(sender:AnyObject) {
        myVideos = []
        self.requestForCollections(token: "")
        //self.refreshControl?.endRefreshing()
    }
    
    //MARK: Custom Methods
    private func sendMail(videoLink: String?, jsonLink: String?) {
        
        if (MFMailComposeViewController.canSendMail()) {
            print("Can send email.")
            
            let mailComposer = MFMailComposeViewController()
            mailComposer.mailComposeDelegate = self
            
            if let jsonLink = jsonLink, jsonLink != "", let videoLink = videoLink, videoLink != "" {
                //Set the subject
                mailComposer.setSubject("[Visym Collector]: Share video")
                mailComposer.setMessageBody(jsonLink + "<br>" + videoLink, isHTML: true)
            } else {
                //Set the subject
                mailComposer.setSubject("[Visym Collector]: Share video")
                
                let fileUrl =  Utilities.getFileUrlStringFromCache(fileName: jsonName)
                if let jsonData = Utilities.getDataFromCacheDirectoryByName(fileurl: URL(fileURLWithPath: fileUrl), filename: jsonName) {
                    mailComposer.addAttachmentData(jsonData as Data, mimeType: "application/pdf", fileName: "SubjectInfo.json")
                }
                let videoURL = Utilities.getFileUrlStringFromCache(fileName: videoName)
                if let videoData = Utilities.getDataFromCacheDirectoryByName(fileurl: URL(fileURLWithPath: videoURL), filename: videoName) {
                        mailComposer.addAttachmentData(videoData, mimeType: "video/mp4", fileName: "Video.mp4")
                }
            }
            
            //this will compose and present mail to user
            self.present(mailComposer, animated: true, completion: nil)
            
        } else {
            print("email is not supported")
            UIUtilities.showAlertMessage("Message", errorMessage: "Email is not supported", errorAlertActionTitle: "Ok", viewControllerUsed: self)
        }
    }
    
    private func presentActionSheetPicker(info: StrVideoByUploaedDateQQuery.Data.StrVideoByUploaedDateQ.Item) {
        UIUtilities.showActionSheetPicker(nil, message: nil, actionSheetTitle: "Share Video", actionSheetTitle1: "Edit Video", actionSheetTitle2: "Delete Video", viewControllerUsed: self, action: {
            if let jsonLink = info.jsonSharingLink, let videoLink = info.videoSharingLink {
                self.sendMail(videoLink: videoLink, jsonLink: jsonLink)
            } else {
                self.collectionVideoJsonUrl = info.annotationFilePath ?? ""
                self.collectionVideoUrl = info.rawVideoFilePath
                
                self.videoName = self.namingVideoandJson(name: self.collectionVideoUrl)
                self.jsonName = self.namingVideoandJson(name: self.collectionVideoJsonUrl)
                
                if self.validateDownloadVideoAndIDs() {
                    self.isShareVideo = true
                    self.downloadJsonFile()
                } else {
                    self.downloadIncomplete()
                }
            }
        }, action1: {
            self.isEditVideo = true
            self.requestForObjectAndActivityList(videoInfo: info)
        }, action2: {
            
            UIUtilities.showAlertMessageWithTwoActionsAndHandler("Discard", errorMessage: LocalizableString.collectionVideoDeleteMessage.localizedString, errorAlertActionTitle: LocalizableString.ok.localizedString, errorAlertActionTitle2: LocalizableString.cancel.localizedString, viewControllerUsed: self, action1: {
                self.requestForDeleteVideo(id: info.id, videoId: info.videoId, uploadedDate: info.uploadedDate)
            }) {
                
            }
        })
    }
    
    private func namingVideoandJson(name: String) -> String {
        var valName = name
        let arr1 = valName.components(separatedBy: "/")
        if arr1.count > 1 {
            valName = arr1.last ?? ""
            print("valName1---\(valName)")
            return valName
        }
        else {
            return valName
        }
    }
}

//My Video Delegate Methods
extension MyVideosTableViewController: MyVideoDelegate {
    
    func didTapOnMoreBtn(item: StrVideoByUploaedDateQQuery.Data.StrVideoByUploaedDateQ.Item) {
        presentActionSheetPicker(info: item)
    }
}

extension MyVideosTableViewController: MFMailComposeViewControllerDelegate {
    func mailComposeController(_ controller: MFMailComposeViewController, didFinishWith result: MFMailComposeResult, error: Error?) {
        self.dismiss(animated: true, completion: nil)
    }
}

extension MyVideosTableViewController: EmptyVideoViewDelegate {
    
    func didTapOnSubmit() {
        self.tabBarController?.selectedIndex = 1
        
    }
    
    func removeItemFromArrayBasedOnValue() {
        var arrBuff: [StrVideoByUploaedDateQQuery.Data.StrVideoByUploaedDateQ.Item] = []
        print("self.videoToken---\(self.myVideos?.count ?? 0)---")
        let arrMain = self.myVideos
        for i in 0 ... arrMain!.count - 1 {
            if let arr1 = arrMain?[i], let attribute = Int(arr1.queryAttribute) {
                
                if attribute == 1 {
                    arrBuff.append(arr1)
                }
            }
        }
        self.myVideos = arrBuff
        
        if arrBuff.count < 10 && videoToken != "" {
            //self.showProgress()
            self.requestForCollections(token: self.videoToken)
        }
        print("2self.myVideos---\(self.myVideos?.count ?? 0)")
        
        if self.myVideos?.count ?? 0 == 0  && self.videoToken == "" {
            print("1---11")
            self.myVideos = []
            self.tableView.reloadData()
            self.footer()
        }
        else {
            self.tableView.reloadData()
            self.footer()
        }
        
        //        self.tableView.reloadData()
        //        print("arrBuff---\(arrBuff.count)---\(arrMain?.count)")
    }
}

//MARK: Api Call
extension MyVideosTableViewController {
    
    private func requestForCollections(token: String) {
        if token == "" && !(self.refreshControl?.isRefreshing ?? false) {
            self.showProgress()
            self.view.isUserInteractionEnabled = false
            self.tabBarController?.tabBar.isUserInteractionEnabled = false
            self.myVideos = []
        }
        
        VideoAPI.getCollectionVideos(token: token) { [weak self] (result, error, token) in
            guard let self = self else { return }
            if let err = error {
                DispatchQueue.main.async {
                    self.refreshControl?.endRefreshing()
                    self.hideProgress()
                    self.tabBarController?.tabBar.isUserInteractionEnabled = true
                    self.view.isUserInteractionEnabled = true
                }
                self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
            } else {
                self.myVideos?.append(contentsOf: result ?? [])
                //print("result---\(result)")
                //                print("1self.myVideos---\(self.myVideos?.count)")
                //                print("self.videoToken---\(self.videoToken)")
                
                self.videoToken = "\(token ?? "")"
                if (self.myVideos?.count ?? 0) > 0 {
                    self.removeItemFromArrayBasedOnValue()
                }
                else if self.videoToken != "" {
                    self.view.isUserInteractionEnabled = false
                    self.tabBarController?.tabBar.isUserInteractionEnabled = false
                    self.showProgress()
                    self.requestForCollections(token: self.videoToken)
                }
                else {
                    self.myVideos = []
                    self.tableView.reloadData()
                    self.footer()
                }
                self.refreshControl?.endRefreshing()
                self.hideProgress()
                self.tabBarController?.tabBar.isUserInteractionEnabled = true
                self.view.isUserInteractionEnabled = true
            }
        }
    }
    
    private func requestForDeleteVideo(id: String, videoId: String, uploadedDate:String) {
        //        print("videoId---\(videoId)")
        self.showProgress()
        self.tabBarController?.tabBar.isUserInteractionEnabled = false
        self.view.isUserInteractionEnabled = false
        
        videoDevInput.videoId = videoId
        videoDevInput.id = id
        videoDevInput.uploadedDate = uploadedDate
        VideoAPI.updateVideo(videoDevInput: videoDevInput) { (status, error) in
            self.hideProgress()
            self.tabBarController?.tabBar.isUserInteractionEnabled = true
            self.view.isUserInteractionEnabled = true
            if status {
                UIUtilities.showAlertMessageWithActionHandler(kDeleteCompleted, message: "", buttonTitle: "OK", viewControllerUsed: self) {
                    self.refresh(sender: self)
                }
            } else if let err = error {
                self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
            }
        }
    }
    
    private func requestForObjectAndActivityList(videoInfo: StrVideoByUploaedDateQQuery.Data.StrVideoByUploaedDateQ.Item) {
        
        self.showProgress()
        self.tabBarController?.tabBar.isUserInteractionEnabled = false
        self.view.isUserInteractionEnabled = false
        
        VideoAPI.getEditVideoList(collectionId: videoInfo.collectionId ?? "") { [weak self] (data, error) in
            guard let self = self else { return }
            if let _ = error {
                self.downloadIncomplete()
            } else if let result = data {
                self.arrDropDownObject = result.objectsList == "" ? [] : (result.objectsList?.components(separatedBy: ",") ?? [])
                self.arrDropDownActivity = result.activityShortNames == "" ? [] : (result.activityShortNames?.components(separatedBy: ",") ?? [])
                self.collectionDescription = result.collectionDescription ?? ""
                self.programID = result.id.components(separatedBy: "_")[0]
                self.projectName = result.id.components(separatedBy: "_")[1]
                
                self.collectionVideoJsonUrl = videoInfo.annotationFilePath ?? ""
                self.collectionVideoUrl = videoInfo.rawVideoFilePath
                
                self.videoName = self.namingVideoandJson(name: self.collectionVideoUrl)
                self.jsonName = self.namingVideoandJson(name: self.collectionVideoJsonUrl)
                
                if self.validateDownloadVideoAndIDs() {
                    self.downloadJsonFile()
                }
                else {
                    self.downloadIncomplete()
                }
                
            } else {
                self.downloadIncomplete()
            }
        }
    }
}

//MARK: Downloading the Video On Click
extension MyVideosTableViewController {
    
    private func downloadJsonFile() {
        self.showProgress()
        self.view.isUserInteractionEnabled = false
        self.tabBarController?.tabBar.isUserInteractionEnabled = false
        
        //Remove files
        Utilities.removeFileFromCache(fileName: videoName)//Remove at the Save Video
        Utilities.removeFileFromCache(fileName: jsonName)
        
        let url = Utilities.getFileUrlStringFromCache(fileName: jsonName)
        AWSS3Manager.shared.downloadFile(fileUrlString: url, fileName: self.collectionVideoJsonUrl, bucketName: AppDelegate.s3BucketName ?? "") { (status, error) in
            
            if let statusVal = status, statusVal == true && (error == nil && error?.localizedDescription.isEmpty ?? true ){
                print("True---\(statusVal)")
                self.downloadVideoFile()
            } else {
                self.downloadIncomplete()
            }
        }
    }
    
    private func downloadVideoFile() {
        let url = Utilities.getFileUrlStringFromCache(fileName: videoName)
        AWSS3Manager.shared.downloadFile(fileUrlString: url, fileName: self.collectionVideoUrl, bucketName: AppDelegate.s3BucketName ?? "") { (status, error) in
            
            if let statusVal = status, statusVal == true && (error == nil && error?.localizedDescription.isEmpty ?? true ) {
                print("True---\(statusVal)")
                if self.isShareVideo {
                    self.hideProgress()
                    self.isShareVideo = false
                    self.view.isUserInteractionEnabled = true
                    self.tabBarController?.tabBar.isUserInteractionEnabled = true
                    self.sendMail(videoLink: nil, jsonLink: nil)
                } else {
                    self.loadServerDemoVideo()
                }
            } else {
                self.downloadIncomplete()
            }
        }
    }
    
    func loadServerDemoVideo() {
        let fileUrl =  Utilities.getFileUrlStringFromCache(fileName: jsonName)
        if let jsonData = Utilities.getDataFromCacheDirectoryByName(fileurl: URL(fileURLWithPath: fileUrl), filename: jsonName) {
            loadPreviewInfoDetails(jsonData: jsonData)
        }
        else {
            self.downloadIncomplete()
        }
    }
    
    func downloadIncomplete() {//Load Project Video
        self.hideProgress()
        self.isEditVideo = false
        self.isShareVideo = false
        DispatchQueue.main.async {
            UIUtilities.showAlertMessageWithActionHandler(kDownloadFailed, message: "", buttonTitle: "OK", viewControllerUsed: self) {
                //                        self.navigateToNextScreen()
                
                self.tabBarController?.tabBar.isUserInteractionEnabled = true
                self.view.isUserInteractionEnabled = true
            }
        }
        
        Utilities.removeFileFromCache(fileName: videoName)//Remove at the Save Video
        Utilities.removeFileFromCache(fileName: jsonName)
    }
    
    private func validateDownloadVideoAndIDs() -> Bool {
        if !(collectionVideoJsonUrl.isEmpty || collectionVideoUrl.isEmpty) {
            return true
        }
        return false
    }
    
    
    
    private func loadPreviewInfoDetails(jsonData: Data) {
        let videoURL = URL(fileURLWithPath: Utilities.getFileUrlStringFromCache(fileName: videoName))
        let fileUrl =  Utilities.getFileUrlStringFromCache(fileName: jsonName)
        
        let decoder = JSONDecoder()
        do {
            let info = try decoder.decode(WelcomeAtDecode.self, from: jsonData)
            
            if !info.metadata.orientation.isEmpty {
                VideoVariables.deviceOrientationRawValue = Utilities.getOrientationInInt(value: info.metadata.orientation)
                
                self.navigateToEditVideo(fileUrl: fileUrl, videoURL: videoURL)
            } else {
                self.downloadIncomplete()
            }
        } catch {
            print(error.localizedDescription)
            self.downloadIncomplete()
        }
    }
    
    func loadNextTrainingScreen(fileUrl: String, videoURL: URL,serverTraining: Bool) {
        DispatchQueue.main.async {
            let lastViewController = self.navigationController?.viewControllers.last
            if !(lastViewController?.isKind(of: ConsentVideoPreviewVC.self))! {
                
                print("VideoVariables.deviceOrientationRawValue---\(VideoVariables.deviceOrientationRawValue)")
                
                self.view.isUserInteractionEnabled = true
                self.hideProgress()
                self.tabBarController?.tabBar.isUserInteractionEnabled = true
                
                let authStoryBoard = UIStoryboard(name: "Activity", bundle: nil)
                let previewVC = authStoryBoard.instantiateViewController(withIdentifier: "ConsentVideoPreviewVC") as! ConsentVideoPreviewVC
                
                previewVC.fileUrl = fileUrl
                previewVC.fileName = self.jsonName
                previewVC.videoURL = videoURL
                previewVC.previewFlow = .MyVideos
                previewVC.serverTraining = serverTraining
                previewVC.hidesBottomBarWhenPushed = true
                self.navigationController?.isNavigationBarHidden = true
                self.navigationController?.pushViewController(previewVC, animated: true)
            }
        }
    }
    
    func navigateToEditVideo(fileUrl: String, videoURL: URL) {
        DispatchQueue.main.async {
            let lastViewController = self.navigationController?.viewControllers.last
            if !(lastViewController?.isKind(of: EditVideoVC.self))! {
                                
                self.view.isUserInteractionEnabled = true
                self.hideProgress()
                self.tabBarController?.tabBar.isUserInteractionEnabled = true
                
                let authStoryBoard = UIStoryboard(name: "Activity", bundle: nil)
                let previewVC = authStoryBoard.instantiateViewController(withIdentifier: "EditVideoVC") as! EditVideoVC
                
                if self.isEditVideo {
                    self.isEditVideo = false
                    previewVC.editVideoFlow = .MyVideoEdit
                } else {
                    previewVC.editVideoFlow = .MyVideoPreview
                }
                
                previewVC.fileUrl = fileUrl
                previewVC.fileName = self.jsonName
                previewVC.videoURL = videoURL
                previewVC.arrDropDownObject = self.arrDropDownObject
                previewVC.arrDropDownActivity = self.arrDropDownActivity
                previewVC.projectName = self.projectName
                previewVC.programID = self.programID
                previewVC.collectionDescription = self.collectionDescription
                previewVC.hidesBottomBarWhenPushed = true
                self.navigationController?.isNavigationBarHidden = true
                self.navigationController?.pushViewController(previewVC, animated: true)
            }
        }
    }
}


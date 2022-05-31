//
//  GalleryTableViewController.swift
//  STR
//
//  Created by Srujan on 06/01/20.
//  
//

import UIKit
import MessageUI


class GalleryTableViewController: UITableViewController {

    //MARK: Data Members
    lazy var otherVideos: [StrRatingVideoSortByAssignedDateQuery.Data.StrRatingVideoSortByAssignedDate.Item]? = []
    var selectedVideo: StrRatingVideoSortByAssignedDateQuery.Data.StrRatingVideoSortByAssignedDate.Item?
    var countViewLoad = 0
    lazy var videoDevInput = UpdateStrVideosInput(id: "", videoId: "", uploadedDate: "", queryAttribute: "0")
    lazy var ratingQuestionInstances: [RatingInstance] = []
    
    var collectionVideoUrl = ""
    var collectionVideoJsonUrl = ""
    var videoToken: String = ""
    var videoName: String = ""
    var jsonName: String = ""
    
    var arrOfObject: [String] = []
    
  
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Uncomment the following line to preserve selection between presentations
        // self.clearsSelectionOnViewWillAppear = false
        
        // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
        // self.navigationItem.rightBarButtonItem = self.editButtonItem
        
        setupTableView()
        setupNavBar()

        self.refreshControl?.isEnabled = true
        self.refreshControl?.addTarget(self, action: #selector(refresh), for: UIControl.Event.valueChanged)
        self.tabBarController?.tabBar.isUserInteractionEnabled = true
        UserDefaults.standard.set(false, forKey: UserDefaults.Keys.deleteRatingVideo.rawValue)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        MenuController.panGestureRecognizer?.isEnabled = true
        self.refreshControl?.isEnabled = true
        self.navigationController?.isNavigationBarHidden = false
        if otherVideos?.count ?? 0 == 0 {
            self.requestForRatingVideos(token: "")
        }
        
        if let deleteVideo = UserDefaults.standard.value(forKey: UserDefaults.Keys.deleteRatingVideo.rawValue) as? Bool {
            if let videoIndex = otherVideos?.firstIndex(where: {$0.videoId == selectedVideo?.videoId}), deleteVideo {
                otherVideos?.remove(at: videoIndex)
                self.tableView.reloadData()
                self.footer()
                selectedVideo = nil
            }
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
        
        self.title = "RATINGS" //Set Title
        self.navigationController?.navigationBar.titleTextAttributes = attributes
        self.navigationController?.navigationBar.shouldRemoveShadow(true)
        return barButton
    }
    
    @objc func sideMenuAction(_ sender:AnyObject) {
        sideMenuController?.revealMenu()
    }
    
    private func footer() {
        if otherVideos?.count ?? 0 == 0  && videoToken == ""{
            guard let usernameView = EmptyVideoView.instanceFromNib()
                else { return }
            usernameView.lblDescriptionLb.text = LocalizableString.noRatingVideosMsg.localizedString
            usernameView.lblNoVideosLb.text = "No Ratings"
            usernameView.delegate = self
            usernameView.btnCaptureActivity.isHidden = true
            self.tableView.tableFooterView = usernameView
        }
        else {
            self.tableView.tableFooterView = UIView()
        }
    }
    
    private func getInstanceID(arrOfInstance: String) {
        let instances = arrOfInstance.replacingOccurrences(of: "{", with: "%").replacingOccurrences(of: "}", with: "%").getExtractedArrOfString()
        for instance in instances {
            print(instance)
            let instanceObj = RatingInstance(id: (instance + ",").slice(from: "instance_id=", to: ",") ?? "",
                                             question: (instance + ",").slice(from: "question=", to: ",") ?? "",
                                             frameValue: (instance + ",").slice(from: "show_at_frame=", to: ",") ?? "0",
                                             thumpsDownId: (instance + ",").slice(from: "no=", to: ",") ?? "0",
                                             collectionId: selectedVideo?.collectionId ?? "",
                                             collectionName: selectedVideo?.collectionName ?? "",
                                             programId: selectedVideo?.programId ?? "",
                                             projectId: selectedVideo?.projectId ?? "",
                                             projectName: selectedVideo?.programName ?? "",
                                             videoId: selectedVideo?.videoId ?? "",
                                             week: (instance + ",").slice(from: "week=", to: ",") ?? "",
                                             questionShortName: (instance + ",").slice(from: "question_short_name=", to: ",") ?? "",
                                             submittedTime: "",
                                             videoUploadedTime: (instance + ",").slice(from: "video_uploaded_date=", to: ",") ?? ""
                                             )
            ratingQuestionInstances.append(instanceObj)
        }
    }
    
    // MARK: - Table view data source
    override func numberOfSections(in tableView: UITableView) -> Int {
        // #warning Incomplete implementation, return the number of sections
        return 1
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        // #warning Incomplete implementation, return the number of rows
      return otherVideos?.count ?? 0
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        if let cell = tableView.dequeueReusableCell(withIdentifier: VideoTableViewCell.reuseIdentifier, for: indexPath) as? VideoTableViewCell, otherVideos?.count ?? 0 > 0 {
            
            let video = otherVideos?[indexPath.row]
            
            if (video?.thumbnailSmall ?? "").isEmpty {
                cell.noTumbnailView.isHidden = false
                //                cell.buttonVideoPreview.isEnabled = false
                //                cell.isUserInteractionEnabled = false
            } else {
                cell.noTumbnailView.isHidden = true
                //                cell.buttonVideoPreview.isEnabled = true
                //                cell.isUserInteractionEnabled = true
                let tumbnailImageURLString = "\(video?.thumbnailSmall ?? "")"
                cell.previewImageView.setImageUsingUrl(tumbnailImageURLString)
            }
            
            cell.idLbl.text = video?.collectionName ?? ""
            cell.labelStatusHeight.constant = 0
//            cell.activityTypelbl.text = "\(removeExtrasActivitiesList(text: video?.activitiesList ?? ""))"
            cell.createdDatelbl.text = DateHelper.getVideoCollectedDate(date: video?.uploadedDate ?? "")
            /*let percentage = video?.ratingScore ?? ""
            cell.likePercentLbl.text = percentage + "%"
            
            if percentage.isEmpty {*/
            cell.buttonLikePercentHt.constant = 0
            cell.buttonLikePercentBt.constant = 5
            cell.buttonLikePercent.setImage(UIImage(), for: .normal)
            cell.likePercentLbl.text = ""
           // }
            
            var duration = Double(video?.duration ?? "0") ?? 0
            duration = duration.truncate(places: 0)
            let durationVal = DateHelper.convertformat2(second: duration)
            
            cell.videoDurationLbl.text = "\(durationVal)    "
            
            cell.buttonMore.isHidden = true
            
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
  
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        
        if let videos = otherVideos, videos.count > 0 {
            selectedVideo = videos[indexPath.row]
            collectionVideoJsonUrl = selectedVideo?.annotationFilePath ?? ""
            collectionVideoUrl = selectedVideo?.videoFilePath ?? ""
            print("collectionVideoJsonUrl---\(collectionVideoJsonUrl)")
            print("collectionVideoUrl---\(collectionVideoUrl)")
            
            self.videoName = self.namingVideoandJson(name: self.collectionVideoUrl)
            self.jsonName = self.namingVideoandJson(name: self.collectionVideoJsonUrl)
            self.requestForObjectAndActivityList()
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
                self.requestForRatingVideos(token: self.videoToken)
            }
        }
    }
  
    //MARK: IBActions
    @objc func refresh(sender:AnyObject) {
        self.otherVideos = []
        self.requestForRatingVideos(token: "")
        //self.refreshControl?.endRefreshing()
    }
    
    //MARK: Custom Methods
    
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

extension GalleryTableViewController: MFMailComposeViewControllerDelegate {
  func mailComposeController(_ controller: MFMailComposeViewController, didFinishWith result: MFMailComposeResult, error: Error?) {
    self.dismiss(animated: true, completion: nil)
  }
}

extension GalleryTableViewController: EmptyVideoViewDelegate {
    
    func didTapOnSubmit() {
        self.tabBarController?.selectedIndex = 1
    }
    
    func removeItemFromArrayBasedOnValue() {
        //var arrBuff: [StrVideosByQueryAttributeQuery.Data.StrVideosByQueryAttribute.Item] = []
        //let arrMain = self.otherVideoss
//        for i in 0 ... arrMain!.count - 1 {
//            if let arr1 = arrMain?[i], let attribute = Int(arr1.queryAttribute) {
//
//                if attribute == 1 {
//                    arrBuff.append(arr1)
//                }
//            }
//        }
//        self.otherVideoss = arrBuff
        
        if self.otherVideos?.count ?? 0 < 10 && videoToken != "" {
            self.showProgress()
            self.requestForRatingVideos(token: self.videoToken)
        }
        
        if self.otherVideos?.count ?? 0 == 0  && self.videoToken == "" {
            self.footer()
        }
        else {
            self.tableView.reloadData()
            self.footer()
        }
    }
}

//MARK: Api Call
extension GalleryTableViewController {
    
    private func requestForRatingVideos(token: String) {
        if token == "" && !(self.refreshControl?.isRefreshing ?? false) {
            DispatchQueue.main.async {
                self.showProgress()
                self.tabBarController?.tabBar.isUserInteractionEnabled = false
                self.view.isUserInteractionEnabled = false
                self.otherVideos = []
            }
        }
                
        RatingAPI.getRatingVideos(token: token) {[weak self] (result, error, token) in
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
                self.otherVideos?.append(contentsOf: result ?? [])
                
                self.videoToken = "\(token ?? "")"
                if (self.otherVideos?.count ?? 0) > 0 {
                    self.removeItemFromArrayBasedOnValue()
                } else if self.videoToken != "" {
                    self.showProgress()
                    self.view.isUserInteractionEnabled = false
                    self.tabBarController?.tabBar.isUserInteractionEnabled = false
                    
                    self.requestForRatingVideos(token: self.videoToken)
                } else {
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
    
    private func requestForRatingResponse() {
        
        self.showProgress()
        
        RatingAPI.getRatingResponseList(videoId: selectedVideo?.videoId ?? "") { [weak self] (result, error) in
            guard let self = self else { return }
            if let responses = result {
                let result = self.ratingQuestionInstances.map { (response) -> RatingInstance in
                    if let _ = responses.filter({$0.id == response.id}).first {
                        var localResponse = response
                        localResponse.update = true
                        return localResponse
                    } else {
                        return response
                    }
                }
                self.ratingQuestionInstances = result
            }
            
            if self.validateDownloadVideoAndIDs() {
                if !(self.collectionVideoJsonUrl.isEmpty) {
                    self.downloadJsonFile()
                } else {
                    self.downloadVideoFile()
                }
                
            } else {
                self.downloadIncomplete()
            }
        }
    }
    
    private func requestForObjectAndActivityList() {
        
        self.showProgress()
        self.tabBarController?.tabBar.isUserInteractionEnabled = false
        self.view.isUserInteractionEnabled = false
        
        VideoAPI.getEditVideoList(collectionId: selectedVideo?.collectionId ?? "") { [weak self] (data, error) in
            guard let self = self else { return }
            if let _ = error {
                self.downloadIncomplete()
            } else if let result = data {
                self.arrOfObject = result.objectsList == "" ? [] : (result.objectsList?.components(separatedBy: ",") ?? [])
                
                self.ratingQuestionInstances = []
                self.getInstanceID(arrOfInstance: self.selectedVideo?.instanceIds ?? "")
                self.requestForRatingResponse()
            } else {
                self.downloadIncomplete()
            }
        }
    }
}

//MARK: Downloading the Video On Click
extension GalleryTableViewController {
    
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
                self.downloadVideoFile()
            }
        }
    }
    
    private func downloadVideoFile() {
        
        let url = Utilities.getFileUrlStringFromCache(fileName: videoName)
        AWSS3Manager.shared.downloadFile(fileUrlString: url, fileName: self.collectionVideoUrl, bucketName: AppDelegate.s3BucketName ?? "") { (status, error) in
            
            if let statusVal = status, statusVal == true && (error == nil && error?.localizedDescription.isEmpty ?? true ) {
                print("True---\(statusVal)")
                self.loadServerDemoVideo()
            } else {
                self.downloadIncomplete()
            }
        }
    }
    
    func loadServerDemoVideo() {
        
        let fileUrl =  Utilities.getFileUrlStringFromCache(fileName: jsonName)
        
        if jsonName == "" {
            VideoVariables.deviceOrientationRawValue = Utilities.getOrientationInInt(value: selectedVideo?.orientation ?? "") //selectedVideo?.orientation ?? ""
            let videoURL = URL(fileURLWithPath: Utilities.getFileUrlStringFromCache(fileName: videoName))
            self.loadNextTrainingScreen(fileUrl: fileUrl, videoURL: videoURL, serverTraining: true)
        } else {
            if let jsonData = Utilities.getDataFromCacheDirectoryByName(fileurl: URL(fileURLWithPath: fileUrl), filename: jsonName) {
                loadPreviewInfoDetails(jsonData: jsonData)
            }
            else {
                self.downloadIncomplete()
            }
        }
    }
    
    func downloadIncomplete() {//Load Project Video
        DispatchQueue.main.async {
            self.hideProgress()
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
        if !(collectionVideoUrl.isEmpty) {
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
                                
                self.loadNextTrainingScreen(fileUrl: fileUrl, videoURL: videoURL, serverTraining: true)
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
                
                UserDefaults.standard.set(false, forKey: UserDefaults.Keys.deleteRatingVideo.rawValue)
                
                self.view.isUserInteractionEnabled = true
                self.hideProgress()
                self.tabBarController?.tabBar.isUserInteractionEnabled = true
                
                let authStoryBoard = UIStoryboard(name: "Activity", bundle: nil)
                let previewVC = authStoryBoard.instantiateViewController(withIdentifier: "ConsentVideoPreviewVC") as! ConsentVideoPreviewVC
                previewVC.arrOfObject = self.arrOfObject
                previewVC.fileUrl = fileUrl
                previewVC.fileName = self.jsonName
                previewVC.videoURL = videoURL
                previewVC.previewFlow = .Ratings
                previewVC.ratingQuestionInstances = self.ratingQuestionInstances
                previewVC.serverTraining = serverTraining
                previewVC.hidesBottomBarWhenPushed = true
                self.navigationController?.isNavigationBarHidden = true
                self.navigationController?.pushViewController(previewVC, animated: true)
            }
        }
    }
}

struct RatingInstance {
    let id: String
    let question: String
    let frameValue: String
    let thumpsDownId: String
    let collectionId: String
    let collectionName: String
    let programId: String
    let projectId: String
    let projectName: String
    let videoId: String
    let week: String
    let questionShortName: String
    let submittedTime: String
    let videoUploadedTime: String
    var update = false
    
}

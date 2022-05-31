//
//  VideoTableViewCell.swift
//  STR
//
//  Created by Srujan on 06/01/20.
//  
//

import UIKit
import AVFoundation

protocol MyVideoDelegate: class {
  // 
    func didTapOnMoreBtn(item: StrVideoByUploaedDateQQuery.Data.StrVideoByUploaedDateQ.Item)
}

class VideoTableViewCell: UITableViewCell {
    
    // MARK:- Outlets
    @IBOutlet weak var videoPreviewView: UIView!
    @IBOutlet weak var noTumbnailView: UIView!
    @IBOutlet weak var statusLbl: UILabel!
    @IBOutlet weak var idLbl: UILabel!
    @IBOutlet weak var createdDatelbl: UILabel!
    @IBOutlet weak var videoDurationLbl: UILabel!
    @IBOutlet weak var likePercentLbl: UILabel!
    @IBOutlet weak var noTumbnail1Lbl: UILabel!
    @IBOutlet weak var noTumbnail2Lbl: UILabel!
    @IBOutlet weak var buttonLikePercent: UIButton!
    @IBOutlet weak var buttonLikePercentHt: NSLayoutConstraint!
    @IBOutlet weak var buttonLikePercentBt: NSLayoutConstraint!
    @IBOutlet weak var labelStatusHeight: NSLayoutConstraint!
    @IBOutlet weak var previewImageView: UIImageView!
    @IBOutlet weak var noTumbnailImageView: UIImageView!
    @IBOutlet weak var buttonMore: UIButton!
    @IBOutlet weak var buttonVideoPreview: UIButton!
    
//    var id: String = ""//For the Deletion
//    var videoId: String = ""
//    var uploadedDate: String = ""
//    var rawJsonFilePath: String = ""// Video Play
//    var rawVideoFilePath: String = ""
    var videoInfo: StrVideoByUploaedDateQQuery.Data.StrVideoByUploaedDateQ.Item?
    
    //MARK: Data Members
    weak var delegate: MyVideoDelegate?
  
    // MARK:- Actions
    
    @IBAction func moreButtonPressed(_ sender: UIButton) {
        //
        delegate?.didTapOnMoreBtn(item: videoInfo!)
    }
    
    @IBAction func videoPreviewBtnPressed(_ sender: UIButton) {
        // open the preview
    }
    
    
    
}

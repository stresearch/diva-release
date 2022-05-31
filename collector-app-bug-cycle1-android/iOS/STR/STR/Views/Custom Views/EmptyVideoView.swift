//
//  EmptyVideoView.swift
//  STR
//
//  Created by GovindPrasadC on 5/5/20.
//  
//

import UIKit

class EmptyVideoView: UIView {
    
    @IBOutlet weak var imgNoVideos: UIImageView!
    @IBOutlet weak var lblNoVideosLb: UILabel!
    @IBOutlet weak var lblDescriptionLb: UILabel!
    @IBOutlet weak var btnCaptureActivity: UIButton!
    
    weak var delegate: EmptyVideoViewDelegate?
    
    deinit { Log("\(self) I'm gone ") }
    
    @IBAction func ActionBtnCaptureActivity(_ sender: Any) {
        delegate?.didTapOnSubmit()
    }
    
    class func instanceFromNib() -> EmptyVideoView? {
        let view = UINib(nibName: "EmptyVideoView", bundle: nil).instantiate(
            withOwner: nil,
            options: nil
        ).first as? EmptyVideoView
        return view
    }
    
}

protocol EmptyVideoViewDelegate: class {
    func didTapOnSubmit()
}

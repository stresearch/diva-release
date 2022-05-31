//
//  PreviewScrubBarView.swift
//  STR
//
//  Created by Srujan on 08/01/20.
//  
//

import UIKit

protocol DemoScrubBarDelegate: class {
    func didTapOnTumbsUpOrDown(value: Int)
    func didTapOnSubmitButton()
}

class PreviewScrubBarView: UIView {

    //MARK: Outlets
    @IBOutlet weak var barView: UIView!
    @IBOutlet weak var textViewFeedbackText: UITextView!
    @IBOutlet weak var viewRatingQuestions: UIView!
    @IBOutlet weak var buttonTumbsUp: UIButton!
    @IBOutlet weak var buttonTumbsDown: UIButton!
    @IBOutlet weak var buttonSubmit: UIButton!

    //MARK: Data members
    weak var delegate: DemoScrubBarDelegate?

    class func instanceFromNib() -> PreviewScrubBarView? {
        let view = UINib(nibName: "PreviewScrubBarView", bundle: nil).instantiate(
            withOwner: nil,
            options: nil
        ).first as? PreviewScrubBarView
        return view
    }

    @IBAction func tumbsUpBtnAction(_ sender: UIButton) {
        sender.isSelected.toggle()
//        if sender.isSelected {
//          buttonTumbsUp.setImage(#imageLiteral(resourceName: "thumb_up"), for: .normal)
//        } else {
//          buttonTumbsUp.setImage(#imageLiteral(resourceName: "unselect_thumb_up"), for: .normal)
//        }
        delegate?.didTapOnTumbsUpOrDown(value: 1)
    }

    @IBAction func tumbsDownBtnAction(_ sender: UIButton) {
        sender.isSelected.toggle()
//        if sender.isSelected {
//          buttonTumbsDown.setImage(#imageLiteral(resourceName: "thumb_down"), for: .normal)
//        } else {
//          buttonTumbsDown.setImage(#imageLiteral(resourceName: "unselect_thumb_down"), for: .normal)
//        }
        delegate?.didTapOnTumbsUpOrDown(value: 0)
    }

    @IBAction func rewindBtnAction(_ sender: UIButton) {
        buttonTumbsUp.setImage(#imageLiteral(resourceName: "unselect_thumb_up"), for: .normal)
        buttonTumbsDown.setImage(#imageLiteral(resourceName: "unselect_thumb_down"), for: .normal)
    }
  
    //MARK: Button Actions
    @IBAction func submitBtnAction(_ sender: UIButton) {
        delegate?.didTapOnSubmitButton()
    }
}

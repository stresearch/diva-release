//
//  ConsentQuestionView.swift
//  STR
//
//  Created by Srujan on 03/01/20.
//  
//

import UIKit

protocol ConsentQuestionViewDelegate: class {
    func didAgree(for question: ConsentQuestion?)
    func didDisAgree(for question: ConsentQuestion?)
}

class ConsentQuestionView: UIView {

    @IBOutlet weak var questionLbl: UILabel!
    @IBOutlet weak var agreeBtn: UIButton!
    @IBOutlet weak var disAgreeBtn: UIButton!
    
    weak var delegate: ConsentQuestionViewDelegate?
    private var currentQuestion: ConsentQuestion?
    
    deinit { Log("\(self) I'm gone ") }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    class func instanceFromNib() -> ConsentQuestionView? {
        let view = UINib(nibName: "ConsentQuestionView", bundle: nil).instantiate(
            withOwner: nil,
            options: nil
        ).first as? ConsentQuestionView
        return view
    }
    
    func configureView(with question: ConsentQuestion) {
        self.questionLbl.text = question.short_description
        self.currentQuestion = question
        
        if let agreeID = question.agreeTargetID,
            agreeID == "0" {
            self.disAgreeBtn.isHidden = true
            self.agreeBtn.setTitle("Ok", for: .normal)
        } else if let disAgreeID = question.disagreeTargetID,
            disAgreeID == "0" {
            self.agreeBtn.isHidden = true
            self.disAgreeBtn.setTitle("Ok", for: .normal)
        }
    }

    @IBAction func agreeBtnStarted(_ sender: UIButton) {
        self.currentQuestion?.isAgreed = true
        delegate?.didAgree(for: self.currentQuestion)
    }

    @IBAction func disagreeBtnStarted(_ sender: UIButton) {
        self.currentQuestion?.isAgreed = false
        delegate?.didDisAgree(for: self.currentQuestion)
    }
}

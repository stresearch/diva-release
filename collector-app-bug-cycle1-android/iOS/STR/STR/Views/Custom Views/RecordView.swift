//
//  RecordView.swift
//  STR
//
//  Created by Srujan on 08/01/20.
//  
//

import UIKit

class RecordView: UIView {

    @IBOutlet weak var buttonView: UIView!
    @IBOutlet weak var recordButton: CameraButton!
    @IBOutlet weak var timeLbl: UILabel!

    deinit { Log("\(self) I'm gone ") }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        commonInit()
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        commonInit()
    }

    func commonInit() {
        guard let view = loadViewFromNib() else { return }
        view.frame = self.bounds
        self.addSubview(view)
    }
    
    func loadViewFromNib() -> UIView? {
        let view = UINib(nibName: "RecordView", bundle: nil).instantiate(
            withOwner: self,
            options: nil
        ).first as? UIView
        return view
    }
    
    func updateButtonView(isVideoRecording: Bool){
        let titleColor = (isVideoRecording) ? UIColor.white : .darkText
        let title = (isVideoRecording) ? "Stop" : "Start"
        let bgColor = (isVideoRecording) ? UIColor.red : UIColor.white
        self.buttonView.layer.borderColor = bgColor.cgColor
        self.recordButton.setTitleColor(titleColor, for: .normal)
        self.recordButton.backgroundColor = bgColor
        self.recordButton.setTitle(title, for: .normal)
        self.timeLbl.isHidden = !isVideoRecording
        if !isVideoRecording {
            self.timeLbl.text = "00:00"
        }
    }
    
    func enableButtonView(){
        self.recordButton.isEnabled = true
    }
    
    func disableButtonView(){
        self.recordButton.isEnabled = false
    }
    
}

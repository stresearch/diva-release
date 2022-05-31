//
//  CustomPopUp.swift
//  STR
//
//  Created by Srujan on 08/07/20.
//  
//

import Foundation
import UIKit

protocol CustomPopUpDelegate: class {
    func didTapOnOkBtn()
}

class CustomPopUp: UIView {

    //MARK: Outlets
    @IBOutlet weak var titleTextView: UITextView!
    @IBOutlet weak var okBtn: UIButton!
    @IBOutlet weak var viewBackground: UIView!
    @IBOutlet weak var viewHeightConstant: NSLayoutConstraint!
    weak var delegate: CustomPopUpDelegate?
    
    deinit { Log("\(self) I'm gone ") }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        
    }

    class func instanceFromNib() -> CustomPopUp? {
        let view = UINib(nibName: "CustomPopUp", bundle: nil).instantiate(
          withOwner: nil,
          options: nil
        ).first as? CustomPopUp
        return view
    }
    
    //MARK: Actions
    @IBAction func okBtnAction(_ sender: UIButton) {
        delegate?.didTapOnOkBtn()
    }
    
}

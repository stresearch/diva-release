//
//  PreviewScrubBarWithSubmit.swift
//  STR
//
//  Created by Srujan on 09/01/20.
//  
//

import UIKit

protocol ScrubBarDelegate: class {
    func didTapOnSubmit()
}
class PreviewScrubBarWithSubmit: UIView {
    
    //MARK: Outlets
    @IBOutlet weak var barView: UIView!
    @IBOutlet weak var transeperantView: UIView!
    @IBOutlet weak var descriptionLbl: UILabel!
    @IBOutlet weak var buttonSubmit: UIButton!
    
    weak var delegate: ScrubBarDelegate?
    
    class func instanceFromNib() -> PreviewScrubBarWithSubmit? {
        let view = UINib(nibName: "PreviewScrubBarWithSubmit", bundle: nil).instantiate(
            withOwner: nil,
            options: nil
        ).first as? PreviewScrubBarWithSubmit
        return view
    }
    
    //MARK: Button Actions
    @IBAction func submitBtnAction(_ sender: UIButton) {
        delegate?.didTapOnSubmit()
    }
}

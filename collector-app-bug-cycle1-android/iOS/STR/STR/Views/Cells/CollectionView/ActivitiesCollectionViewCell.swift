//
//  ActivitiesCollectionViewCell.swift
//  CIFaceMask
//
//  Created by Srujan on 06/01/20.
//  Copyright © 2020 BTC Soft. All rights reserved.
//

import UIKit

protocol ActivitiesCollectionCellDelegate: class {
    func didTapOnActivity(index: Int)
}

class ActivitiesCollectionViewCell: UICollectionViewCell {
  
  //MARK:- Outlets
  @IBOutlet weak var labelActivity: UILabel!
    
    //MARK: Data Members
    weak var delegate: ActivitiesCollectionCellDelegate?
    var inderPathRow: Int?
    
    @IBAction func activityButtonAction(_ sender : UIButton) {
        delegate?.didTapOnActivity(index: inderPathRow ?? 0)
    }
  
}

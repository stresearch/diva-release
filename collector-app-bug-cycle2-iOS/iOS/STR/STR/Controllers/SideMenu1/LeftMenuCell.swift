//
//  LeftMenuCell.swift
//  STR
//
//  Created by admin on 15/5/20.
//  Copyright © 2020 BTC. All rights reserved.
//

import UIKit

class LeftMenuCell: UITableViewCell {

  @IBOutlet var menuIcon: UIImageView?
  @IBOutlet var labelTitle: UILabel?
  @IBOutlet var labelSubTitle: UILabel?

  /// Used to populate Cell UI
  /// - Parameter data: Containse data in form of Dictionary
  func populateCellData(data: [String: Any]) {
    menuIcon?.image = UIImage(named: data["iconName"] as! String)
    labelTitle?.text = data["menuTitle"] as? String
  }
}

class LeftMenuResourceTableViewCell: UITableViewCell {

  @IBOutlet var menuIcon: UIImageView?
  @IBOutlet var labelTitle: UILabel?
  @IBOutlet var labelCounter: UILabel?

  /// Used to populate Cell UI
  /// - Parameter data: Containse data in form of Dictionary
  func populateCellData(data: [String: Any]) {
    menuIcon?.image = UIImage.init(named: data["iconName"] as! String)
    labelTitle?.text = data["menuTitle"] as? String
  }
}

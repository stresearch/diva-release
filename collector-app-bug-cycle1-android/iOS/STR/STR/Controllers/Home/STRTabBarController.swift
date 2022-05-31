//
//  STRTabBarController.swift
//  STR
//
//  Created by Surender on 05/03/20.
//  
//

import UIKit

class STRTabBarController: UITabBarController, UITabBarControllerDelegate {
    
    var valPreviousSelectedIndex = -1
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.selectedIndex = 0
        
        //        if  let arrayOfTabBarItems = self.tabBar.items as AnyObject as? NSArray,let tabBarItem = arrayOfTabBarItems[0] as? UITabBarItem {
        //            tabBarItem.isEnabled = false
        //        }
        
        self.delegate = self
        removeTabbarItemsText()
    }
    
    func removeTabbarItemsText() {
        if let items = tabBar.items {
            for item in items {
                item.title = ""
            }
        }
    }
    
    
}

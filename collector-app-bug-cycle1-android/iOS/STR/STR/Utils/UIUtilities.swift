//
//  UIUtilities.swift
//  AcuityLinkHiAdmin
//
//  Created by Srujan on 1/28/19.
//  Copyright © 2019 Boston Technology Corporation. All rights reserved.
//

import UIKit

public typealias AlertAction = () -> Void

class UIUtilities: NSObject {

    // MARK: Methods to show alert messages

    /**
     *   Used to show alert message having two actions
     *   @param  title for the alert,
     *           message for the alert,
     *           titles for the actions,
     *           controller on which alert has to be shown
     *           actions to be performed
     */

    class func showAlertMessageWithTwoActionsAndHandler(
        _ errorTitle: String,
        errorMessage: String,
        errorAlertActionTitle: String,
        errorAlertActionTitle2: String?,
        viewControllerUsed: UIViewController,
        action1: @escaping AlertAction,
        action2: @escaping AlertAction
    ) {

        let alert = UIAlertController(
            title: errorTitle,
            message: errorMessage,
            preferredStyle: UIAlertController.Style.alert
        )

        alert.addAction(
            UIAlertAction(
                title: errorAlertActionTitle,
                style: UIAlertAction.Style.default,
                handler: { (_) in
                    action1()
                }
            )
        )
        if errorAlertActionTitle2 != nil {
            alert.addAction(
                UIAlertAction(
                    title: errorAlertActionTitle2,
                    style: UIAlertAction.Style.default,
                    handler: { (_) in
                        action2()
                    }
                )
            )
        }
        viewControllerUsed.present(alert, animated: true, completion: nil)
    }

    /**
     *   Used to show alert message having one action
     *   @param  title for the alert,
     *           message for the alert,
     *           title for the action,
     *           controller on which alert has to be shown
     *           action to be performed
     */

    class func showAlertMessageWithActionHandler(
        _ title: String,
        message: String,
        buttonTitle: String,
        viewControllerUsed: UIViewController,
        action: @escaping AlertAction
    ) {

        let alert = UIAlertController(
            title: title,
            message: message,
            preferredStyle: UIAlertController.Style.alert
        )

        alert.addAction(
            UIAlertAction(
                title: buttonTitle,
                style: UIAlertAction.Style.default,
                handler: { (_) in
                    action()
                }
            )
        )
        viewControllerUsed.present(alert, animated: true, completion: nil)
    }

    /**
     *   Used to show alert message having one action
     *   @param  title for the alert,
     *           message for the alert,
     *           title for the action,
     *           controller on which alert has to be shown
     */

    class func showAlertMessage(
        _ errorTitle: String,
        errorMessage: String,
        errorAlertActionTitle: String,
        viewControllerUsed: UIViewController?
    ) {

        let alert = UIAlertController(
            title: errorTitle,
            message: errorMessage,
            preferredStyle: UIAlertController.Style.alert
        )
        alert.addAction(
            UIAlertAction(
                title: errorAlertActionTitle,
                style: UIAlertAction.Style.default,
                handler: nil
            )
        )
        viewControllerUsed!.present(alert, animated: true, completion: nil)
    }

    class func showActionSheetPicker(
        _ title: String?,
        message: String?,
        actionSheetTitle: String,
        actionSheetTitle1: String,
        actionSheetTitle2: String,
        viewControllerUsed: UIViewController,
        action: @escaping AlertAction,
        action1: @escaping AlertAction,
        action2: @escaping AlertAction
    ) {
        let alert = UIAlertController(
                   title: title,
                   message: message,
                   preferredStyle: UIAlertController.Style.actionSheet
               )
               alert.addAction(
                   UIAlertAction(
                       title: actionSheetTitle,
                       style: UIAlertAction.Style.default,
                       handler: { (_) in
                           action()
                       }
                   )
               )
        alert.addAction(
            UIAlertAction(
                title: actionSheetTitle1,
                style: UIAlertAction.Style.default,
                handler: { (_) in
                    action1()
                }
            )
        )
        alert.addAction(
            UIAlertAction(
                title: actionSheetTitle2,
                style: UIAlertAction.Style.default,
                handler: { (_) in
                    action2()
                }
            )
        )
        alert.addAction(
          UIAlertAction(
              title: "Cancel",
              style: UIAlertAction.Style.cancel,
              handler: nil
          )
      )
               viewControllerUsed.present(alert, animated: true, completion: nil)
    }
    
}

struct AppUtility {

    static func lockOrientation(_ orientation: UIInterfaceOrientationMask) {

        if let delegate = UIApplication.shared.delegate as? AppDelegate {
            delegate.orientationLock = orientation
        }
    }

    /// OPTIONAL Added method to adjust lock and rotate to the desired orientation
    static func lockOrientation(_ orientation: UIInterfaceOrientationMask, andRotateTo rotateOrientation:UIInterfaceOrientation) {

        self.lockOrientation(orientation)

        UIDevice.current.setValue(rotateOrientation.rawValue, forKey: "orientation")
    }

}

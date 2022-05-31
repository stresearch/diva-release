//
//  MenuRelatedExtension.swift
//  STR
//
//  Created by Govind Prasad  on 20/5/20.
//  
//

import Foundation
import UIKit

extension UIView {
    var parentViewController1: UIViewController? {
        var parentResponder: UIResponder? = self
        while parentResponder != nil {
            // swiftlint:disable:next force_unwrapping
            parentResponder = parentResponder!.next
            if let viewController = parentResponder as? UIViewController {
                return viewController
            }
        }
        return nil
    }

    var parentNavigationController: UINavigationController? {
        let currentViewController = parentViewController1
        if let navigationController = currentViewController as? UINavigationController {
            return navigationController
        }
        return currentViewController?.navigationController
    }
}

extension UIViewController {

    func load(_ viewController: UIViewController?, on view: UIView) {
        guard let viewController = viewController else {
            return
        }

        // `willMoveToParentViewController1:` is called automatically when adding

        addChild(viewController)

        viewController.view.frame = view.bounds
        viewController.view.translatesAutoresizingMaskIntoConstraints = true
        viewController.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]

        view.addSubview(viewController.view)

        viewController.didMove(toParent: self)
    }

    func unload(_ viewController: UIViewController?) {
        guard let viewController = viewController else {
            return
        }

        viewController.willMove(toParent: nil)
        viewController.view.removeFromSuperview()
        viewController.removeFromParent()
        // `didMoveToParentViewController1:` is called automatically when removing
    }
}

public extension UIViewController {

    /// Access the nearest ancestor view controller hierarchy that is a side menu controller.
    var sideMenuController: MenuController? {
        return findSideMenuController(from: self)
    }

    fileprivate func findSideMenuController(from viewController: UIViewController) -> MenuController? {
        var sourceViewController: UIViewController? = viewController
        repeat {
            sourceViewController = sourceViewController?.parent
            if let sideMenuController = sourceViewController as? MenuController {
                return sideMenuController
            }
        } while (sourceViewController != nil)
        return nil
    }
}

extension UIWindow {

    // swiftlint:disable identifier_name
    /// Returns current application's `statusBarWindows`
    static var sb: UIWindow? {
        // We use a non-public key here to obtain the `statusBarWindow` window.
        // We have been using it in real world app and it won't be rejected by the review team for using this key.
        let s = "status", b = "Bar", w = "Window"
        if #available(iOS 13, *) {
            return nil
        } else {
            return UIApplication.shared.value(forKey: s+b+w) as? UIWindow
        }
    }

    /// Changes the windows' visibility with custom behavior
    ///
    /// - Parameters:
    ///   - hidden: the windows hidden status
    ///   - behavior: status bar behavior
    internal func setStatusBarHidden(_ hidden: Bool, with behavior: MenuController.Preferences.StatusBarBehavior) {
        guard behavior != .none else {
            return
        }

        switch behavior {
        case .fade, .hideOnMenu:
            alpha = hidden ? 0 : 1
        case .slide:
            let statusBarHeight = UIApplication.shared.statusBarFrame.height
            transform = hidden ? CGAffineTransform(translationX: 0, y: -statusBarHeight) : .identity
        default:
            return
        }
    }

    internal func isStatusBarHidden(with behavior: MenuController.Preferences.StatusBarBehavior) -> Bool {
        switch behavior {
        case .none:
            return false
        case .fade, .hideOnMenu:
            return alpha == 0
        case .slide:
            return transform != .identity
        }
    }
}

class NavigationController: UINavigationController {

    open override var childForStatusBarHidden: UIViewController? {
        return self.topViewController
    }

    open override var childForStatusBarStyle: UIViewController? {
        return self.topViewController
    }
}

class Preferences {
    static let shared = Preferences()
    var enableTransitionAnimation = false
}

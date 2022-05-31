//
//  MenuDelegate.swift
//  STR
//
//  Created by admin on 20/5/20.
//  
//

import Foundation
import UIKit

// Delegate Methods
public protocol SideMenuControllerDelegate: class {

    // MARK: Animation

    /// Called to allow the delegate to return a non-interactive animator object for use during view controller transitions.
    /// Same with UIKit's `navigationController:animationControllerForOperation:fromViewController:toViewController:`.
    ///
    /// - Parameters:
    ///   - sideMenuController: The side menu controller
    ///   - fromVC: The currently visible view controller.
    ///   - toVC: The view controller that should be visible at the end of the transition.
    /// - Returns: The animator object responsible for managing the transition animations,
    ///            or nil if you want to use the fade transitions.
    func sideMenuController(_ sideMenuController: MenuController,
                            animationControllerFrom fromVC: UIViewController,
                            to toVC: UIViewController) -> UIViewControllerAnimatedTransitioning?

    // MARK: Switching

    /// Side menu will show a view controller.
    ///
    /// - Parameters:
    ///   - sideMenuController: current side menu controller
    ///   - viewController: the view controller to show
    ///   - animated: whether it's animated
    func sideMenuController(_ sideMenuController: MenuController, willShow viewController: UIViewController, animated: Bool)

    /// Side menu did showed a view controller.
    ///
    /// - Parameters:
    ///   - sideMenuController: current side menu controller
    ///   - viewController: the view controller shown
    ///   - animated: whether it's animated
    func sideMenuController(_ sideMenuController: MenuController, didShow viewController: UIViewController, animated: Bool)

    // MARK: Revealing

    /// Side menu is going to reveal.
    ///
    /// - Parameter sideMenu: The side menu
    func sideMenuControllerWillRevealMenu(_ sideMenuController: MenuController)

    /// Side menu did revealed.
    ///
    /// - Parameter sideMenu: The side menu
    func sideMenuControllerDidRevealMenu(_ sideMenuController: MenuController)

    /// Side menu is going to hide.
    ///
    /// - Parameter sideMenu: The side menu
    func sideMenuControllerWillHideMenu(_ sideMenuController: MenuController)

    /// Side menu did hided.
    ///
    /// - Parameter sideMenu: The side menu
    func sideMenuControllerDidHideMenu(_ sideMenuController: MenuController)
}

// Provides default implementation for delegates
public extension SideMenuControllerDelegate {
    func sideMenuController(_ sideMenuController: MenuController,
                            animationControllerFrom fromVC: UIViewController,
                            to toVC: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        return nil
    }

    func sideMenuController(_ sideMenuController: MenuController,
                            willShow viewController: UIViewController,
                            animated: Bool) {}
    func sideMenuController(_ sideMenuController: MenuController,
                            didShow viewController: UIViewController,
                            animated: Bool) {}
    func sideMenuControllerWillRevealMenu(_ sideMenuController: MenuController) {}
    func sideMenuControllerDidRevealMenu(_ sideMenuController: MenuController) {}
    func sideMenuControllerWillHideMenu(_ sideMenuController: MenuController) {}
    func sideMenuControllerDidHideMenu(_ sideMenuController: MenuController) {}
}

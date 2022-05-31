//
//  Storyboard+Extensions.swift
//  STR
//
//  Created by Srujan on 31/12/19.
//  
//

import UIKit

protocol StoryboardIdentifiable {
    static var storyboardIdentifier: String { get }
}

extension StoryboardIdentifiable where Self: UIViewController {
    static var storyboardIdentifier: String {
        return String(describing: self)
    }
}

extension UIViewController: StoryboardIdentifiable { }

extension UIStoryboard {
    enum Storyboard: String {
        case launchScreen
        case main
        case auth
        case activity
        var filename: String {
            return rawValue.capitalized
        }
    }
    convenience init(_ storyboard: Storyboard) {
        self.init(name: storyboard.filename, bundle: Bundle.main)
    }
    func instantiateViewController<T: UIViewController>() -> T {
        guard let viewController = self.instantiateViewController(withIdentifier: T.storyboardIdentifier) as? T else {
            fatalError("Couldn't instantiate view controller with identifier \(T.storyboardIdentifier) ")
        }
        return viewController
    }
}

extension UIViewController {
    
    func presentViewController(withStoryboardName storyboard: String, withIdentifier identifier: String? = nil){
        let sb = UIStoryboard(name: storyboard, bundle: Bundle.main)
        var viewController: UIViewController?
        if identifier != nil {
            let vc = sb.instantiateViewController(withIdentifier: identifier!)
            viewController = vc
        }else {
            if let initial = sb.instantiateInitialViewController(){
                viewController = initial
            }
        }
        if viewController != nil {
            present(viewController!, animated: true, completion: nil)
        }
    }
    
}

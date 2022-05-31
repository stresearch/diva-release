//
//  UIView+Extensions.swift
//  STR
//
//  Created by Srujan on 18/06/19.
//  
//

import Foundation
import UIKit

//  MARK: - Closure Configuration
//  These methods provide some syntactic sugar for configuring views with closures.

protocol ClosureConfigurable {}

extension ClosureConfigurable where Self: UIView {

    /**
     Inititalizes any `UIView` subclass with a closure for configuration.
     - Parameter configure: The closure for configuring the view.
     */

    init(_ configure: (Self) -> Void) {
        self.init(frame: .zero)
        configure(self)
    }

    /**
     Provides a closure for configuring any `UIView` subclass.
     - Parameter configuration: The closure for configuring the view.
     */

    func applying(configuration configure: (Self) -> Void) -> Self {
        configure(self)
        return self
    }

}
extension UIView: ClosureConfigurable {}

// MARK: - Auto Layout Helpers

extension UIView {

    /// Size of view.
    var size: CGSize {
        get { return self.frame.size }
        set {
            self.width = newValue.width
            self.height = newValue.height
        }
    }

    /// Width of view.
    var width: CGFloat {
        get { return self.frame.size.width }
        set { self.frame.size.width = newValue }
    }

    /// Height of view.
    var height: CGFloat {
        get { return self.frame.size.height }
        set { self.frame.size.height = newValue }
    }

    /** Convenience method for programatically adding a subview with constraints. */

    func addSubview(_ view: UIView, constraints: [NSLayoutConstraint]) {
        addSubview(view)
        view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate(constraints)
    }

    /// This is a function to get subViews of a particular type from view recursively. It would look recursively in all subviews and return back the subviews of the type T
    /// https://stackoverflow.com/a/45297466/5321670
    func allSubViewsOf<T: UIView>(type: T.Type) -> [T] {
        var all = [T]()
        func getSubview(view: UIView) {
            if let aView = view as? T { all.append(aView) }
            guard view.subviews.count>0 else { return }
            view.subviews.forEach { getSubview(view: $0) }
        }
        getSubview(view: self)
        return all
    }

    func roundCorners(corners: UIRectCorner, radius: CGFloat) {
        let path = UIBezierPath(
            roundedRect: bounds,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        let mask = CAShapeLayer()
        mask.path = path.cgPath
        layer.mask = mask
    }

    func addConstraintWithFormat(format: String, views: UIView...) {
        var viewsDictionary = [String: UIView]()
        for (index, view) in views.enumerated() {
            let key = "v\(index)"
            view.translatesAutoresizingMaskIntoConstraints = false
            viewsDictionary[key] = view
        }
        addConstraints(
            NSLayoutConstraint.constraints(
                withVisualFormat: format,
                options: NSLayoutConstraint.FormatOptions(),
                metrics: nil,
                views: viewsDictionary
            )
        )
    }

    func fadeTo(alphaValue: CGFloat, withDuration duration: TimeInterval) {
        UIView.animate(withDuration: duration) { self.alpha = alphaValue }
    }

    /** Loads instance from nib with the same name. */
    func loadNib() -> UIView {
        let bundle = Bundle(for: type(of: self))
        let nibName = type(of: self).description().components(separatedBy: ".").last!
        let nib = UINib(nibName: nibName, bundle: bundle)
        return nib.instantiate(withOwner: self, options: nil).first as! UIView
    }

}

extension UILabel {

    enum Size: Int {
        case title = 50
        case paragraph = 21

        var cgFloat: CGFloat { return CGFloat(rawValue) }
    }

    func setDefaultStyle(size: UILabel.Size, weight: UIFont.Weight) {
        font = UIFont.systemFont(ofSize: size.cgFloat, weight: weight)
    }

}

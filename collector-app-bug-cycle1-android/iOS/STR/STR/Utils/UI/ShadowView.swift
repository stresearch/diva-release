//
//  ShadowView.swift
//  STR
//
//  Created by Srujan on 10/06/19.
//  
//
import UIKit

class ShadowView: UIView {

    override func layoutSubviews() {
        super.layoutSubviews()

        setupShadow()
    }

    lazy private var _cornerRadius: CGFloat = 0
    lazy private var _borderWidth: CGFloat = 0
    private var _borderColor: CGColor?
    lazy private var _shadowRadius: CGFloat = 0
    lazy private var _shadowOpacity: Float = 0
    lazy private var _shadowOffset: CGSize = .zero
    private var _shadowColor: CGColor?

    @IBInspectable override var cornerRadius: CGFloat {
        get { return _cornerRadius }
        set { _cornerRadius = newValue }
    }

    @IBInspectable override var borderWidth: CGFloat {
        get { return _borderWidth }
        set { _borderWidth = newValue }
    }

    @IBInspectable override var borderColor: UIColor? {
        get {
            if let color = _borderColor { return UIColor(cgColor: color) }
            return nil
        }
        set { _borderColor = newValue?.cgColor }
    }

    @IBInspectable override var shadowRadius: CGFloat {
        get { return _shadowRadius }
        set { _shadowRadius = newValue }
    }

    @IBInspectable var shadowOpacity: Float {
        get { return _shadowOpacity }
        set { _shadowOpacity = newValue }
    }

    @IBInspectable var shadowOffset: CGSize {
        get { return _shadowOffset }
        set { _shadowOffset = newValue }
    }

    @IBInspectable var shadowColor: UIColor? {
        get {
            if let color = _shadowColor { return UIColor(cgColor: color) }
            return nil
        }
        set { _shadowColor = newValue?.cgColor }
    }

    private func setupShadow() {
        self.layer.cornerRadius = _cornerRadius
        self.layer.shadowOffset = _shadowOffset
        self.layer.shadowRadius = _shadowRadius
        self.layer.shadowOpacity = _shadowOpacity
        self.layer.borderColor = _borderColor
        self.layer.borderWidth = _borderWidth
        if cornerRadius != 0 {
            self.layer.shadowPath = UIBezierPath(
                roundedRect: self.bounds,
                byRoundingCorners: .allCorners,
                cornerRadii: CGSize(width: _cornerRadius, height: _cornerRadius)
            ).cgPath
        } else { self.layer.shadowPath = UIBezierPath(rect: bounds).cgPath }
        self.layer.shouldRasterize = true
        self.layer.rasterizationScale = UIScreen.main.scale
        self.layer.masksToBounds = false
    }
}

class ShadowButton: UIButton {

    override func layoutSubviews() {
        super.layoutSubviews()

        setupShadow()
    }

    lazy private var _cornerRadius: CGFloat = 0
    lazy private var _borderWidth: CGFloat = 0
    private var _borderColor: CGColor?
    lazy private var _shadowRadius: CGFloat = 0
    lazy private var _shadowOpacity: Float = 0
    lazy private var _shadowOffset: CGSize = .zero
    private var _shadowColor: CGColor?
    private var shadowLayer: CAShapeLayer!

    @IBInspectable override var cornerRadius: CGFloat {
        get { return _cornerRadius }
        set { _cornerRadius = newValue }
    }

    @IBInspectable override var borderWidth: CGFloat {
        get { return _borderWidth }
        set { _borderWidth = newValue }
    }

    @IBInspectable override var borderColor: UIColor? {
        get {
            if let color = _borderColor { return UIColor(cgColor: color) }
            return nil
        }
        set { _borderColor = newValue?.cgColor }
    }

    @IBInspectable override var shadowRadius: CGFloat {
        get { return _shadowRadius }
        set { _shadowRadius = newValue }
    }

    @IBInspectable var shadowOpacity: Float {
        get { return _shadowOpacity }
        set { _shadowOpacity = newValue }
    }

    @IBInspectable var shadowOffset: CGSize {
        get { return _shadowOffset }
        set { _shadowOffset = newValue }
    }

    @IBInspectable var shadowColor: UIColor? {
        get {
            if let color = _shadowColor { return UIColor(cgColor: color) }
            return nil
        }
        set { _shadowColor = newValue?.cgColor }
    }

    private func setupShadow() {
        self.layer.cornerRadius = _cornerRadius
        self.layer.shadowOffset = _shadowOffset
        self.layer.shadowRadius = _shadowRadius
        self.layer.shadowOpacity = _shadowOpacity
        self.layer.borderColor = _borderColor
        self.layer.borderWidth = _borderWidth
        if cornerRadius != 0 {
            self.layer.shadowPath = UIBezierPath(
                roundedRect: self.bounds,
                byRoundingCorners: .allCorners,
                cornerRadii: CGSize(width: _cornerRadius, height: _cornerRadius)
            ).cgPath
        } else { self.layer.shadowPath = UIBezierPath(rect: bounds).cgPath }
        self.layer.shouldRasterize = true
        self.layer.rasterizationScale = UIScreen.main.scale
        self.layer.masksToBounds = false
    }
}

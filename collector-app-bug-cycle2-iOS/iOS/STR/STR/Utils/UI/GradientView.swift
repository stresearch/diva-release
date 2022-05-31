//
//  GradientView.swift
//  STR
//
//  Created by Srujan on 12/06/19.
//  
//

import UIKit
import UIKit

/// Simple view for drawing gradients and borders.

class GradientView: UIView {

    private var gradientLayer: CAGradientLayer!

    @IBInspectable var topColor: UIColor = .red { didSet { setNeedsLayout() } }

    @IBInspectable var bottomColor: UIColor = .yellow { didSet { setNeedsLayout() } }

    @IBInspectable var shadowColor: UIColor = .clear { didSet { setNeedsLayout() } }

    @IBInspectable var shadowX: CGFloat = 0 { didSet { setNeedsLayout() } }

    @IBInspectable var shadowY: CGFloat = -3 { didSet { setNeedsLayout() } }

    @IBInspectable var shadowBlur: CGFloat = 3 { didSet { setNeedsLayout() } }

    @IBInspectable var startPointX: CGFloat = 0 { didSet { setNeedsLayout() } }

    @IBInspectable var startPointY: CGFloat = 0.5 { didSet { setNeedsLayout() } }

    @IBInspectable var endPointX: CGFloat = 1 { didSet { setNeedsLayout() } }

    @IBInspectable var endPointY: CGFloat = 0.5 { didSet { setNeedsLayout() } }

    override class var layerClass: AnyClass { return CAGradientLayer.self }

    override func layoutSubviews() {
        self.gradientLayer = self.layer as? CAGradientLayer
        self.gradientLayer.colors = [topColor.cgColor, bottomColor.cgColor]
        self.gradientLayer.startPoint = CGPoint(x: startPointX, y: startPointY)
        self.gradientLayer.endPoint = CGPoint(x: endPointX, y: endPointY)
        self.layer.shadowColor = shadowColor.cgColor
        self.layer.shadowOffset = CGSize(width: shadowX, height: shadowY)
        self.layer.shadowRadius = shadowBlur
        self.layer.shadowOpacity = 1

    }

    func animate(duration: TimeInterval, newTopColor: UIColor, newBottomColor: UIColor) {
        let fromColors = self.gradientLayer?.colors
        let toColors: [AnyObject] = [newTopColor.cgColor, newBottomColor.cgColor,]
        self.gradientLayer?.colors = toColors
        let animation: CABasicAnimation = CABasicAnimation(keyPath: "colors")
        animation.fromValue = fromColors
        animation.toValue = toColors
        animation.duration = duration
        animation.isRemovedOnCompletion = true
        animation.fillMode = .forwards
        animation.timingFunction = CAMediaTimingFunction(name: CAMediaTimingFunctionName.linear)
        self.gradientLayer?.add(animation, forKey: "animateGradient")
    }
}

//@IBDesignable
class GradientButton: UIButton {

    @IBInspectable var startColor: UIColor = UIColor.white { didSet { setupView() } }

    @IBInspectable var endColor: UIColor = UIColor.black { didSet { setupView() } }

    @IBInspectable var roundness: CGFloat = 0.0 { didSet { setupView() } }

    @IBInspectable var startPointX: CGFloat = 0 { didSet { setNeedsLayout() } }

    @IBInspectable var startPointY: CGFloat = 0.5 { didSet { setNeedsLayout() } }

    @IBInspectable var endPointX: CGFloat = 1 { didSet { setNeedsLayout() } }

    @IBInspectable var endPointY: CGFloat = 0.5 { didSet { setNeedsLayout() } }

    // MARK: Overrides ******************************************

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setupView()
    }

    // MARK: Internal functions *********************************
    override class var layerClass: AnyClass { return CAGradientLayer.self }

    // Setup the view appearance
    private func setupView() {

        guard let gradientLayer = self.gradientLayer else { return }
        let colors: Array = [startColor.cgColor, endColor.cgColor]
        gradientLayer.colors = colors
        gradientLayer.cornerRadius = roundness

        gradientLayer.startPoint = CGPoint(x: startPointX, y: startPointY)
        gradientLayer.endPoint = CGPoint(x: endPointX, y: endPointY)

        self.setNeedsDisplay()

    }

    // Helper to return the main layer as CAGradientLayer

    private var gradientLayer: CAGradientLayer? { return self.layer as? CAGradientLayer }
}

import cv2
import numpy as np

def adaptive_threshold(image: np.ndarray) -> np.ndarray:
    """
    Applies adaptive thresholding to remove non-uniform illumination and shadows.
    """
    if len(image.shape) == 3:
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    else:
        gray = image
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    thresh = cv2.adaptiveThreshold(
        blurred, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2
    )
    return thresh

def detect_table_lines(image: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    """
    Uses morphological matrix line checks to isolate horizontal and vertical table grid lines.
    """
    if len(image.shape) == 3:
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    else:
        gray = image
    thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)[1]
    
    # Horizontal line kernel
    h_kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (25, 1))
    h_lines = cv2.morphologyEx(thresh, cv2.MORPH_OPEN, h_kernel, iterations=2)
    
    # Vertical line kernel
    v_kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (1, 25))
    v_lines = cv2.morphologyEx(thresh, cv2.MORPH_OPEN, v_kernel, iterations=2)
    
    return h_lines, v_lines

def get_skew_angle(image: np.ndarray) -> float:
    h, w = image.shape[:2]
    max_dim = max(h, w)
    
    # Downscale for fast skew angle estimation without losing precision
    if max_dim > 1000:
        scale = 1000.0 / max_dim
        small = cv2.resize(image, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_AREA)
    else:
        small = image

    gray = cv2.cvtColor(small, cv2.COLOR_BGR2GRAY)
    blur = cv2.GaussianBlur(gray, (9, 9), 0)
    thresh = cv2.threshold(blur, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)[1]
    
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (30, 5))
    dilate = cv2.dilate(thresh, kernel, iterations=3)
    
    contours, _ = cv2.findContours(dilate, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
    angles = []
    
    for contour in contours:
        area = cv2.contourArea(contour)
        if area < 50:
            continue
        
        rect = cv2.minAreaRect(contour)
        angle = rect[-1]
        
        if angle < -45:
            angle = 90 + angle
        elif angle > 45:
            angle = angle - 90
            
        angles.append(angle)
        
    if not angles:
        return 0.0
        
    return float(np.median(angles))

def deskew(image: np.ndarray) -> np.ndarray:
    angle = get_skew_angle(image)
    if abs(angle) < 0.5:
        return image
        
    (h, w) = image.shape[:2]
    center = (w // 2, h // 2)
    M = cv2.getRotationMatrix2D(center, angle, 1.0)
    
    # Expand target canvas dimensions to prevent corner text cropping
    cos = np.abs(M[0, 0])
    sin = np.abs(M[0, 1])
    new_w = int((h * sin) + (w * cos))
    new_h = int((h * cos) + (w * sin))
    
    M[0, 2] += (new_w / 2) - center[0]
    M[1, 2] += (new_h / 2) - center[1]
    
    rotated = cv2.warpAffine(
        image, M, (new_w, new_h), flags=cv2.INTER_CUBIC, borderValue=(255, 255, 255)
    )
    return rotated


import cv2
import numpy as np
from pupil_detectors import Detector2D

def convert_to_grayscale(image_bytes, height, width, channels):
    detector = Detector2D()

    # Convert byte array to a numpy array
    np_array = np.frombuffer(bytes(image_bytes), np.uint8)

    # Decode the numpy array into an OpenCV image matrix
    img1 = np_array.reshape((width, height, channels)) #cv2.imdecode(np_array, cv2.IMREAD_COLOR)
    img = cv2.cvtColor(img1, cv2.COLOR_BGRA2RGBA)

    # Apply OpenCV operations (e.g., Grayscale conversion)
    gray = cv2.cvtColor(img, cv2.COLOR_RGBA2GRAY)
    result = detector.detect(gray)
    ellipse = result["ellipse"]

    # draw the ellipse outline onto the input image
    # note that cv2.ellipse() cannot deal with float values
    # also it expects the axes to be semi-axes (half the size)
    cv2.ellipse(
       img,
       tuple(int(v) for v in ellipse["center"]),
       tuple(int(v / 2) for v in ellipse["axes"]),
       ellipse["angle"],
       0, 360, # start/end angle for drawing
       (255, 0, 255, 255), # color (BGR): red
       5
    )

    # Encode the processed image back into bytes
    _, encoded_img = cv2.imencode('.png', img)

    # Return raw bytes back to Android
    return encoded_img.tobytes()


def convert_to_grayscale2(image_bytes):
    detector = Detector2D()

    # Convert byte array to a numpy array
    np_array = np.frombuffer(bytes(image_bytes), np.uint8)

    # Decode the numpy array into an OpenCV image matrix
    img = cv2.imdecode(np_array, cv2.IMREAD_COLOR)

    # Apply OpenCV operations (e.g., Grayscale conversion)
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    result = detector.detect(gray)
    ellipse = result["ellipse"]

    # draw the ellipse outline onto the input image
    # note that cv2.ellipse() cannot deal with float values
    # also it expects the axes to be semi-axes (half the size)
    cv2.ellipse(
    img,
    tuple(int(v) for v in ellipse["center"]),
    tuple(int(v / 2) for v in ellipse["axes"]),
    ellipse["angle"],
    0, 360, # start/end angle for drawing
    (255, 0, 255), # color (BGR): red
    10
    )

    # Encode the processed image back into bytes
    _, encoded_img = cv2.imencode('.png', img)

    # Return raw bytes back to Android
    return encoded_img.tobytes()
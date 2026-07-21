from fastapi import APIRouter, File, UploadFile, HTTPException
from services.ocr_manager import OcrManager
from utils.image_processing import deskew
import cv2
import numpy as np

router = APIRouter()
ocr_manager = OcrManager()

@router.post("/api/v1/ocr/process")
async def process_ocr(file: UploadFile = File(...)):
    filename = file.filename.lower()
    if not (filename.endswith('.jpg') or filename.endswith('.jpeg') or filename.endswith('.png')):
        raise HTTPException(
            status_code=400,
            detail="Invalid file extension. Only .jpg, .jpeg, and .png extensions are allowed."
        )

    contents = await file.read()
    if len(contents) > 10 * 1024 * 1024:
        raise HTTPException(
            status_code=400,
            detail="File size exceeds maximum allowed limit of 10MB."
        )

    nparr = np.frombuffer(contents, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    if img is None:
        raise HTTPException(
            status_code=400,
            detail="Invalid image file. Could not decode."
        )

    deskewed_img = deskew(img)
    parsed_data = ocr_manager.parse_table(deskewed_img)
    #parsed_data = ocr_manager.parse_table(img)
    return parsed_data

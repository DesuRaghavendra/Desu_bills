import React, { useState } from 'react';
import {
  Container,
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Select,
  MenuItem,
  FormControl,
  Alert,
  Stack,
  CircularProgress
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { createTable, previewNewTable } from '../services/tableService';

export default function CreateTable() {
  const navigate = useNavigate();
  const [tableName, setTableName] = useState('');
  const [headers, setHeaders] = useState([]);
  const [suggestedTypes, setSuggestedTypes] = useState({});
  const [rows, setRows] = useState([]);
  const [isDataLoaded, setIsDataLoaded] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  
  const [loading, setLoading] = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // File selection
  const handleFileChange = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      setSelectedFile(e.target.files[0]);
      setError(null);
    }
  };

  // Upload file to preview new table
  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Please select an image file first');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await previewNewTable(selectedFile);
      setHeaders(data.headers || []);
      setSuggestedTypes(data.suggestedTypes || {});
      setRows(data.rows || []);
      
      // Default table name to filename without extension + ' Staging'
      const baseName = selectedFile.name.substring(0, selectedFile.name.lastIndexOf('.')) || 'Uploaded Table';
      setTableName(baseName + ' Staging');
      setIsDataLoaded(true);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to process table image. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Skip and load static mockup data directly
  const handleLoadMockData = () => {
    setHeaders(['Item Code', 'Cost', 'In Stock']);
    setSuggestedTypes({
      'Item Code': 'string',
      'Cost': 'decimal',
      'In Stock': 'string'
    });
    setRows([
      ['A102', '299.50', 'true'],
      ['B504', '19.99', 'false']
    ]);
    setTableName('Mock Table Staging');
    setIsDataLoaded(true);
    setError(null);
  };

  // Edit cell value
  const handleCellChange = (rowIndex, colIndex, newVal) => {
    const updatedRows = rows.map((row, rIdx) => {
      if (rIdx === rowIndex) {
        return row.map((cell, cIdx) => (cIdx === colIndex ? newVal : cell));
      }
      return row;
    });
    setRows(updatedRows);
  };

  // Edit header name
  const handleHeaderChange = (colIndex, newName) => {
    if (!newName || newName.trim() === '') return;
    const oldName = headers[colIndex];
    if (oldName === newName) return;

    if (headers.includes(newName)) {
      setError('Column header name already exists');
      return;
    }

    const updatedHeaders = headers.map((h, idx) => (idx === colIndex ? newName : h));
    
    const updatedTypes = { ...suggestedTypes };
    updatedTypes[newName] = updatedTypes[oldName] || 'string';
    delete updatedTypes[oldName];

    setHeaders(updatedHeaders);
    setSuggestedTypes(updatedTypes);
    setError(null);
  };

  // Change type of column
  const handleTypeChange = (colName, newType) => {
    setSuggestedTypes({
      ...suggestedTypes,
      [colName]: newType
    });
  };

  // Add a new empty row
  const handleAddRow = () => {
    const newRow = Array(headers.length).fill('');
    setRows([...rows, newRow]);
  };

  // Delete specific row
  const handleDeleteRow = (rowIndex) => {
    setRows(rows.filter((_, idx) => idx !== rowIndex));
  };

  // Add a new column
  const handleAddColumn = () => {
    const defaultColName = `Column ${headers.length + 1}`;
    setHeaders([...headers, defaultColName]);
    setSuggestedTypes({
      ...suggestedTypes,
      [defaultColName]: 'string'
    });
    setRows(rows.map(row => [...row, '']));
  };

  // Delete specific column
  const handleDeleteColumn = (colIndex) => {
    const colName = headers[colIndex];
    setHeaders(headers.filter((_, idx) => idx !== colIndex));
    
    const updatedTypes = { ...suggestedTypes };
    delete updatedTypes[colName];
    setSuggestedTypes(updatedTypes);

    setRows(rows.map(row => row.filter((_, idx) => idx !== colIndex)));
  };

  // Save table structure
  const handleSave = async () => {
    if (!tableName.trim()) {
      setError('Table Name cannot be empty');
      return;
    }
    if (headers.length === 0) {
      setError('Table must have at least one column');
      return;
    }

    setSaveLoading(true);
    setError(null);

    // Format request payload schemas & initialRows maps
    const schema = {
      columns: headers.map(header => ({
        name: header,
        type: suggestedTypes[header] || 'string'
      }))
    };

    const initialRows = rows.map(row => {
      const rowMap = {};
      headers.forEach((header, idx) => {
        rowMap[header] = row[idx];
      });
      return rowMap;
    });

    try {
      await createTable({ tableName, schema, initialRows });
      setSuccess('Table saved successfully!');
      setTimeout(() => {
        navigate('/');
      }, 1500);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save table definition. Check validations.');
    } finally {
      setSaveLoading(false);
    }
  };

  const handleCancel = () => {
    navigate('/');
  };

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4, pb: 2, borderBottom: '1px solid #e0e0e0' }}>
        <Typography variant="h5" sx={{ fontWeight: 600, color: '#333' }}>
          Verify and Create Table
        </Typography>
        <Button variant="outlined" color="primary" onClick={handleCancel}>
          Back to Dashboard
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 3 }}>
          {success}
        </Alert>
      )}

      {!isDataLoaded ? (
        <Paper sx={{ p: 4, border: '1px dashed #ccc', textAlign: 'center', bgcolor: '#fafafa' }}>
          <Typography variant="h6" sx={{ mb: 2, color: '#555' }}>
            Upload dynamic spreadsheet image
          </Typography>
          <Box sx={{ mb: 3 }}>
            <input
              accept="image/*"
              style={{ display: 'none' }}
              id="raised-button-file"
              type="file"
              onChange={handleFileChange}
              disabled={loading}
            />
            <label htmlFor="raised-button-file">
              <Button variant="contained" component="span" disabled={loading} sx={{ textTransform: 'none' }}>
                Choose Image File
              </Button>
            </label>
            {selectedFile && (
              <Typography variant="body2" sx={{ mt: 1, color: '#777' }}>
                Selected: {selectedFile.name} ({(selectedFile.size / 1024).toFixed(1)} KB)
              </Typography>
            )}
          </Box>

          <Stack direction="row" spacing={2} justifyContent="center">
            <Button
              variant="contained"
              color="primary"
              onClick={handleUpload}
              disabled={loading || !selectedFile}
              sx={{ textTransform: 'none', px: 4 }}
            >
              {loading ? (
                <>
                  <CircularProgress size={20} color="inherit" sx={{ mr: 1 }} />
                  Processing...
                </>
              ) : (
                'Process Image'
              )}
            </Button>
            <Button
              variant="outlined"
              color="secondary"
              onClick={handleLoadMockData}
              disabled={loading}
              sx={{ textTransform: 'none' }}
            >
              Load Mock OCR Grid
            </Button>
          </Stack>
        </Paper>
      ) : (
        <>
          <Paper sx={{ p: 3, mb: 4, boxShadow: 'none', border: '1px solid #e0e0e0' }}>
            <Typography variant="h6" sx={{ mb: 2, fontWeight: 550 }}>
              Table Configuration
            </Typography>
            <TextField
              label="Table Name"
              value={tableName}
              onChange={(e) => setTableName(e.target.value)}
              fullWidth
              variant="outlined"
              size="small"
              sx={{ mb: 2 }}
              disabled={saveLoading}
            />
          </Paper>

          <Paper sx={{ p: 3, boxShadow: 'none', border: '1px solid #e0e0e0', mb: 4 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
              <Typography variant="h6" sx={{ fontWeight: 550 }}>
                Staging Grid Preview
              </Typography>
              <Stack direction="row" spacing={1}>
                <Button size="small" onClick={handleAddColumn} variant="outlined" disabled={saveLoading} sx={{ textTransform: 'none' }}>
                  + Add Column
                </Button>
                <Button size="small" onClick={handleAddRow} variant="outlined" disabled={saveLoading} sx={{ textTransform: 'none' }}>
                  + Add Row
                </Button>
              </Stack>
            </Box>

            <TableContainer sx={{ border: '1px solid #e0e0e0', borderRadius: '4px', overflowX: 'auto' }}>
              <Table size="small">
                <TableHead sx={{ bgcolor: '#f9f9f9' }}>
                  <TableRow>
                    {headers.map((header, idx) => (
                      <TableCell key={idx} sx={{ minWidth: 150, borderRight: '1px solid #e0e0e0' }}>
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                          <TextField
                            value={header}
                            onChange={(e) => handleHeaderChange(idx, e.target.value)}
                            variant="standard"
                            size="small"
                            inputProps={{ style: { fontWeight: 'bold', fontSize: '14px' } }}
                            sx={{ mb: 0.5 }}
                            disabled={saveLoading}
                          />
                          <FormControl size="small" fullWidth>
                            <Select
                              value={suggestedTypes[header] || 'string'}
                              onChange={(e) => handleTypeChange(header, e.target.value)}
                              sx={{ fontSize: '12px', height: '28px' }}
                              disabled={saveLoading}
                            >
                              <MenuItem value="string">string</MenuItem>
                              <MenuItem value="decimal">decimal</MenuItem>
                            </Select>
                          </FormControl>
                          <Button 
                            color="error" 
                            size="small" 
                            onClick={() => handleDeleteColumn(idx)}
                            disabled={saveLoading}
                            sx={{ textTransform: 'none', fontSize: '10px', p: 0, minWidth: 0, alignSelf: 'flex-start' }}
                          >
                            Remove Col
                          </Button>
                        </Box>
                      </TableCell>
                    ))}
                    <TableCell sx={{ width: 80 }} align="center">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row, rIdx) => (
                    <TableRow key={rIdx}>
                      {row.map((cell, cIdx) => (
                        <TableCell key={cIdx} sx={{ borderRight: '1px solid #e0e0e0', p: 0.5 }}>
                          <TextField
                            value={cell}
                            onChange={(e) => handleCellChange(rIdx, cIdx, e.target.value)}
                            fullWidth
                            variant="standard"
                            InputProps={{ disableUnderline: true }}
                            sx={{ px: 1 }}
                            disabled={saveLoading}
                          />
                        </TableCell>
                      ))}
                      <TableCell align="center">
                        <Button
                          color="error"
                          size="small"
                          onClick={() => handleDeleteRow(rIdx)}
                          disabled={saveLoading}
                          sx={{ textTransform: 'none', minWidth: 0, p: 0.5 }}
                        >
                          Delete
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Paper>

          <Box sx={{ display: 'flex', gap: 2 }}>
            <Button
              variant="contained"
              color="success"
              onClick={handleSave}
              disabled={saveLoading}
              sx={{ textTransform: 'none' }}
            >
              {saveLoading ? (
                <>
                  <CircularProgress size={20} color="inherit" sx={{ mr: 1 }} />
                  Saving...
                </>
              ) : (
                'Save Table'
              )}
            </Button>
            <Button variant="outlined" color="error" onClick={handleCancel} disabled={saveLoading} sx={{ textTransform: 'none' }}>
              Cancel Verification
            </Button>
          </Box>
        </>
      )}
    </Container>
  );
}

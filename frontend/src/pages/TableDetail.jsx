import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Container,
  Box,
  Typography,
  Paper,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Alert,
  CircularProgress,
  Stack,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  Checkbox,
  Tooltip,
  IconButton
} from '@mui/material';
import { getTableDetail, getTableRecords, previewExistingTable, appendRecords, searchRecords, updateRecord, deleteRecord, batchDeleteRecords } from '../services/tableService';
import SearchPanel from '../components/SearchPanel';

export default function TableDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [table, setTable] = useState(null);
  const [records, setRecords] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  const [loading, setLoading] = useState(true);
  const [recordsLoading, setRecordsLoading] = useState(false);
  const [error, setError] = useState(null);

  // Search state
  const [searchMode, setSearchMode] = useState(false);
  const [searchFilters, setSearchFilters] = useState(null);
  const [searchLoading, setSearchLoading] = useState(false);
  const [success, setSuccess] = useState(null);

  // Upload modal states
  const [uploadOpen, setUploadOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploadLoading, setUploadLoading] = useState(false);

  // Staged data from OCR
  const [rawMappedRows, setRawMappedRows] = useState([]);
  const [unmappedColumns, setUnmappedColumns] = useState([]);
  
  // Mapping modal state
  const [mappingOpen, setMappingOpen] = useState(false);
  const [columnMappings, setColumnMappings] = useState({}); // ocrHeader -> targetHeader

  // Review staging modal state
  const [reviewOpen, setReviewOpen] = useState(false);
  const [stagedRows, setStagedRows] = useState([]); // Aligned rows ready to append
  const [appendLoading, setAppendLoading] = useState(false);

  // CRUD editing state
  const [editingCell, setEditingCell] = useState(null); // { recordId, colName }
  const [editValue, setEditValue] = useState('');
  const [editError, setEditError] = useState(null);
  const [savingRecordId, setSavingRecordId] = useState(null);

  // Multi-select state
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [batchDeleting, setBatchDeleting] = useState(false);

  const fetchTableDetail = async () => {
    try {
      setLoading(true);
      const detail = await getTableDetail(id);
      setTable(detail);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to retrieve table structure');
    } finally {
      setLoading(false);
    }
  };

  const fetchRecords = async () => {
    try {
      setRecordsLoading(true);
      const pageData = await getTableRecords(id, page, size);
      setRecords(pageData.content || []);
      setTotalPages(pageData.totalPages || 0);
      setTotalElements(pageData.totalElements || 0);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to retrieve record rows');
    } finally {
      setRecordsLoading(false);
    }
  };

  useEffect(() => {
    fetchTableDetail();
  }, [id]);

  useEffect(() => {
    if (table) {
      if (searchMode && searchFilters) {
        executeSearch(searchFilters, page, size);
      } else {
        fetchRecords();
      }
    }
  }, [table, page, size]);

  // Search handlers
  const handleSearch = async (filters) => {
    setSearchFilters(filters);
    setSearchMode(true);
    setPage(0);
    await executeSearch(filters, 0, size);
  };

  const executeSearch = async (filters, pg, sz) => {
    try {
      setSearchLoading(true);
      setRecordsLoading(true);
      const result = await searchRecords(id, filters, pg, sz);
      setRecords(result.content || []);
      setTotalPages(result.totalPages || 0);
      setTotalElements(result.totalElements || 0);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Search query failed');
    } finally {
      setSearchLoading(false);
      setRecordsLoading(false);
    }
  };

  const handleClearSearch = () => {
    setSearchMode(false);
    setSearchFilters(null);
    setPage(0);
    fetchRecords();
  };

  const handleChangePage = (event, newPage) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event) => {
    setSize(parseInt(event.target.value, 10));
    setPage(0);
  };

  const handleBack = () => {
    navigate('/');
  };

  // Open upload file selection
  const handleOpenUpload = () => {
    setSelectedFile(null);
    setError(null);
    setUploadOpen(true);
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      setSelectedFile(e.target.files[0]);
    }
  };

  // Process selected file via OCR
  const handleProcessImage = async () => {
    if (!selectedFile) {
      setError('Please select an image file first');
      return;
    }
    setUploadLoading(true);
    setError(null);
    try {
      const response = await previewExistingTable(id, selectedFile);
      setRawMappedRows(response.mappedRows || []);
      
      const unmapped = response.unmappedColumns || [];
      setUnmappedColumns(unmapped);

      setUploadOpen(false);

      if (unmapped.length > 0) {
        // Initialize column mapping state
        const initialMappings = {};
        unmapped.forEach(col => {
          initialMappings[col.ocrColumnName] = 'ignore';
        });
        setColumnMappings(initialMappings);
        setMappingOpen(true);
      } else {
        // No unmapped columns - proceed straight to review staging
        setStagedRows(response.mappedRows || []);
        setReviewOpen(true);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to parse image data matching schema.');
    } finally {
      setUploadLoading(false);
    }
  };

  // Skip and load static mocked aligned append rows
  const handleLoadMockAppend = () => {
    const mockRows = [
      { 'Item Code': 'C901', 'Cost': '12.00', 'In Stock': 'true' },
      { 'Item Code': 'D202', 'Cost': '45.50', 'In Stock': 'false' }
    ];
    setStagedRows(mockRows);
    setUploadOpen(false);
    setReviewOpen(true);
  };

  const handleMappingChange = (ocrCol, targetCol) => {
    setColumnMappings({
      ...columnMappings,
      [ocrCol]: targetCol
    });
  };

  // Confirm mapping and construct aligned staged rows
  const handleConfirmMapping = () => {
    const aligned = rawMappedRows.map(row => {
      const newRow = { ...row };
      Object.keys(columnMappings).forEach(ocrCol => {
        const targetCol = columnMappings[ocrCol];
        if (targetCol && targetCol !== 'ignore') {
          newRow[targetCol] = row[ocrCol];
        }
      });
      return newRow;
    });

    setStagedRows(aligned);
    setMappingOpen(false);
    setReviewOpen(true);
  };

  // Save the staged rows to the database
  const handleConfirmAppend = async () => {
    setAppendLoading(true);
    setError(null);

    const columns = table?.schema?.columns || [];

    // Format fields to String mappings for JSR validation matching
    const recordsToAppend = stagedRows.map(row => {
      const recordMap = {};
      columns.forEach(col => {
        const val = row[col.name];
        recordMap[col.name] = val !== undefined && val !== null ? val.toString() : '';
      });
      return recordMap;
    });

    try {
      await appendRecords(id, recordsToAppend);
      setSuccess(`Successfully appended ${recordsToAppend.length} rows to the table!`);
      setReviewOpen(false);
      setPage(0);
      fetchRecords(); // Refresh the dynamic grid list
      setTimeout(() => {
        setSuccess(null);
      }, 3000);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save appended records. Check validations.');
    } finally {
      setAppendLoading(false);
    }
  };

  // Staged append rows handlers
  const handleStagedCellChange = (rowIndex, colName, newVal) => {
    const updated = stagedRows.map((row, rIdx) => {
      if (rIdx === rowIndex) {
        return { ...row, [colName]: newVal };
      }
      return row;
    });
    setStagedRows(updated);
  };

  const handleAddStagedRow = () => {
    const cols = table?.schema?.columns || [];
    const newRow = {};
    cols.forEach(col => {
      newRow[col.name] = '';
    });
    setStagedRows([...stagedRows, newRow]);
  };

  const handleDeleteStagedRow = (rowIndex) => {
    setStagedRows(stagedRows.filter((_, idx) => idx !== rowIndex));
  };

  const formatDate = (isoString) => {
    if (!isoString) return '-';
    const date = new Date(isoString);
    return date.toLocaleDateString();
  };

  // ---- CRUD Handlers ----

  const isNumericType = (type) => {
    const t = (type || '').toLowerCase();
    return t === 'decimal';
  };

  const validateCellValue = (value, type) => {
    const t = (type || '').toLowerCase();
    if (value === '' || value === null || value === undefined) return null;
    const strVal = value.toString().trim();
    if (t === 'decimal') {
      if (!/^-?\d+(\.\d+)?$/.test(strVal)) return 'Must be a valid decimal number';
    }
    return null;
  };

  const handleCellDoubleClick = (recordId, colName, currentValue) => {
    setEditingCell({ recordId, colName });
    setEditValue(currentValue !== undefined && currentValue !== null ? currentValue.toString() : '');
    setEditError(null);
  };

  const handleEditCancel = () => {
    setEditingCell(null);
    setEditValue('');
    setEditError(null);
  };

  const handleEditSave = async (recordId, colName) => {
    const cols = table?.schema?.columns || [];
    const colDef = cols.find(c => c.name === colName);
    const vErr = validateCellValue(editValue, colDef?.type);
    if (vErr) {
      setEditError(vErr);
      return;
    }

    // Build the full row data with the edited cell
    const row = records.find(r => r.recordId === recordId);
    if (!row) return;
    const updatedData = { ...row.data, [colName]: editValue };

    setSavingRecordId(recordId);
    try {
      const result = await updateRecord(recordId, updatedData);
      // Update local state
      setRecords(prev => prev.map(r =>
        r.recordId === recordId ? { ...r, data: result.data } : r
      ));
      setEditingCell(null);
      setEditValue('');
      setEditError(null);
      setSuccess('Cell updated successfully');
      setTimeout(() => setSuccess(null), 2000);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update record');
    } finally {
      setSavingRecordId(null);
    }
  };

  const handleEditKeyDown = (e, recordId, colName) => {
    if (e.key === 'Enter') {
      handleEditSave(recordId, colName);
    } else if (e.key === 'Escape') {
      handleEditCancel();
    }
  };

  const handleDeleteRow = async (recordId) => {
    try {
      await deleteRecord(recordId);
      setRecords(prev => prev.filter(r => r.recordId !== recordId));
      setTotalElements(prev => prev - 1);
      setSelectedIds(prev => {
        const next = new Set(prev);
        next.delete(recordId);
        return next;
      });
      setSuccess('Record deleted');
      setTimeout(() => setSuccess(null), 2000);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete record');
    }
  };

  const handleToggleSelect = (recordId) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(recordId)) {
        next.delete(recordId);
      } else {
        next.add(recordId);
      }
      return next;
    });
  };

  const handleToggleSelectAll = () => {
    if (selectedIds.size === records.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(records.map(r => r.recordId)));
    }
  };

  const handleBatchDelete = async () => {
    if (selectedIds.size === 0) return;
    setBatchDeleting(true);
    try {
      await batchDeleteRecords(Array.from(selectedIds));
      setSuccess(`Deleted ${selectedIds.size} records`);
      setSelectedIds(new Set());
      // Refresh records
      if (searchMode && searchFilters) {
        executeSearch(searchFilters, page, size);
      } else {
        fetchRecords();
      }
      setTimeout(() => setSuccess(null), 2000);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to batch delete records');
    } finally {
      setBatchDeleting(false);
    }
  };

  if (loading) {
    return (
      <Container maxWidth="lg" sx={{ mt: 8, display: 'flex', justifyContent: 'center' }}>
        <CircularProgress />
      </Container>
    );
  }

  const columns = table?.schema?.columns || [];
  const allSelected = records.length > 0 && selectedIds.size === records.length;

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4, pb: 2, borderBottom: '1px solid #e0e0e0' }}>
        <Box>
          <Typography variant="h5" sx={{ fontWeight: 600, color: '#333' }}>
            {table?.tableName}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Table ID: {table?.tableId}
          </Typography>
        </Box>
        <Stack direction="row" spacing={2}>
          <Button variant="contained" color="primary" onClick={handleOpenUpload} sx={{ textTransform: 'none' }}>
            Append Image Records
          </Button>
          <Button variant="outlined" onClick={handleBack} sx={{ textTransform: 'none' }}>
            Back to Workspace
          </Button>
        </Stack>
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

      {/* Columns Schema Definition Details */}
      <Paper sx={{ p: 3, mb: 4, boxShadow: 'none', border: '1px solid #e0e0e0' }}>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: 550 }}>
          Columns Schema Mappings
        </Typography>
        <Stack direction="row" spacing={1.5} useFlexGap flexWrap="wrap">
          {columns.map((col, idx) => (
            <Chip
              key={idx}
              label={`${col.name} (${col.type})`}
              variant="outlined"
              color="primary"
              sx={{ fontWeight: 500 }}
            />
          ))}
        </Stack>
      </Paper>

      {/* Search Panel */}
      {columns.length > 0 && (
        <SearchPanel
          columns={columns}
          onSearch={handleSearch}
          onClear={handleClearSearch}
          loading={searchLoading}
        />
      )}

      {/* Search active indicator */}
      {searchMode && (
        <Alert severity="info" sx={{ mb: 2 }} onClose={handleClearSearch}>
          Showing filtered results ({totalElements} match{totalElements !== 1 ? 'es' : ''}). Click ✕ to clear search.
        </Alert>
      )}

      {/* Batch delete toolbar */}
      {selectedIds.size > 0 && (
        <Paper sx={{ p: 1.5, mb: 1, display: 'flex', alignItems: 'center', justifyContent: 'space-between', bgcolor: '#e3f2fd', border: '1px solid #90caf9', boxShadow: 'none' }}>
          <Typography variant="body2" sx={{ fontWeight: 500 }}>
            {selectedIds.size} row{selectedIds.size !== 1 ? 's' : ''} selected
          </Typography>
          <Button
            variant="contained"
            color="error"
            size="small"
            onClick={handleBatchDelete}
            disabled={batchDeleting}
            sx={{ textTransform: 'none' }}
          >
            {batchDeleting ? <CircularProgress size={16} color="inherit" /> : `Delete Selected (${selectedIds.size})`}
          </Button>
        </Paper>
      )}

      {/* Dynamic Grid Listing */}
      <Paper sx={{ boxShadow: 'none', border: '1px solid #e0e0e0', overflow: 'hidden' }}>
        <TableContainer sx={{ maxHeight: 600 }}>
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow sx={{ bgcolor: '#f9f9f9' }}>
                <TableCell padding="checkbox" sx={{ bgcolor: '#f9f9f9' }}>
                  <Checkbox
                    size="small"
                    checked={allSelected}
                    indeterminate={selectedIds.size > 0 && !allSelected}
                    onChange={handleToggleSelectAll}
                    disabled={records.length === 0}
                  />
                </TableCell>
                {columns.map((col, idx) => (
                  <TableCell key={idx} sx={{ fontWeight: 600, bgcolor: '#f9f9f9', borderRight: '1px solid #e0e0e0' }}>
                    {col.name}
                  </TableCell>
                ))}
                <TableCell sx={{ fontWeight: 600, bgcolor: '#f9f9f9', borderRight: '1px solid #e0e0e0', minWidth: 110 }}>
                  DATE
                </TableCell>
                <TableCell sx={{ fontWeight: 600, bgcolor: '#f9f9f9', width: 60, textAlign: 'center' }}>
                  Actions
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {recordsLoading ? (
                <TableRow>
                  <TableCell colSpan={columns.length + 3} align="center" sx={{ py: 6 }}>
                    <CircularProgress size={30} />
                  </TableCell>
                </TableRow>
              ) : records.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={columns.length + 3} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    No rows persisted for this table yet.
                  </TableCell>
                </TableRow>
              ) : (
                records.map((row) => (
                  <TableRow
                    key={row.recordId}
                    selected={selectedIds.has(row.recordId)}
                    sx={{ '&:hover': { bgcolor: '#fafafa' } }}
                  >
                    <TableCell padding="checkbox">
                      <Checkbox
                        size="small"
                        checked={selectedIds.has(row.recordId)}
                        onChange={() => handleToggleSelect(row.recordId)}
                      />
                    </TableCell>
                    {columns.map((col, idx) => {
                      const val = row.data?.[col.name];
                      const isEditing = editingCell?.recordId === row.recordId && editingCell?.colName === col.name;

                      return (
                        <TableCell
                          key={idx}
                          sx={{
                            borderRight: '1px solid #e0e0e0',
                            cursor: 'pointer',
                            p: isEditing ? 0.5 : undefined,
                            minWidth: 100
                          }}
                          onDoubleClick={() => handleCellDoubleClick(row.recordId, col.name, val)}
                        >
                          {isEditing ? (
                            <Tooltip
                              title={editError || ''}
                              open={!!editError}
                              arrow
                              placement="top"
                            >
                              <TextField
                                size="small"
                                fullWidth
                                autoFocus
                                value={editValue}
                                type={isNumericType(col.type) ? 'text' : 'text'}
                                error={!!editError}
                                onChange={(e) => {
                                  setEditValue(e.target.value);
                                  const vErr = validateCellValue(e.target.value, col.type);
                                  setEditError(vErr);
                                }}
                                onKeyDown={(e) => handleEditKeyDown(e, row.recordId, col.name)}
                                onBlur={() => handleEditSave(row.recordId, col.name)}
                                disabled={savingRecordId === row.recordId}
                                sx={{ '& .MuiInputBase-input': { py: 0.5, px: 1, fontSize: '0.875rem' } }}
                              />
                            </Tooltip>
                          ) : (
                            <span>{val !== undefined && val !== null ? val.toString() : '-'}</span>
                          )}
                        </TableCell>
                      );
                    })}
                    <TableCell sx={{ borderRight: '1px solid #e0e0e0', minWidth: 110 }}>
                      {formatDate(row.updatedAt)}
                    </TableCell>
                    <TableCell sx={{ textAlign: 'center', width: 60 }}>
                      <Tooltip title="Delete row">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => handleDeleteRow(row.recordId)}
                          sx={{ fontSize: '0.9rem' }}
                        >
                          ✕
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>

        <TablePagination
          rowsPerPageOptions={[5, 10, 25, 50]}
          component="div"
          count={totalElements}
          rowsPerPage={size}
          page={page}
          onPageChange={handleChangePage}
          onRowsPerPageChange={handleChangeRowsPerPage}
          sx={{ borderTop: '1px solid #e0e0e0' }}
        />
      </Paper>

      {/* Upload Image Append Modal */}
      <Dialog open={uploadOpen} onClose={() => !uploadLoading && setUploadOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 600 }}>Append records from image</DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" sx={{ mb: 2, color: 'text.secondary' }}>
            Upload an image page matching or approximating the table structure. Words will be parsed and aligned to columns.
          </Typography>
          <Box sx={{ mb: 3, textAlign: 'center', p: 3, border: '1px dashed #ccc', borderRadius: '4px', bgcolor: '#fafafa' }}>
            <input
              accept="image/*"
              style={{ display: 'none' }}
              id="append-file-input"
              type="file"
              onChange={handleFileChange}
              disabled={uploadLoading}
            />
            <label htmlFor="append-file-input">
              <Button variant="contained" component="span" disabled={uploadLoading} sx={{ textTransform: 'none' }}>
                Choose Image File
              </Button>
            </label>
            {selectedFile && (
              <Typography variant="body2" sx={{ mt: 1, fontWeight: 500 }}>
                {selectedFile.name} ({(selectedFile.size / 1024).toFixed(1)} KB)
              </Typography>
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUploadOpen(false)} disabled={uploadLoading} sx={{ textTransform: 'none' }}>
            Cancel
          </Button>
          <Button onClick={handleLoadMockAppend} color="secondary" disabled={uploadLoading} sx={{ textTransform: 'none' }}>
            Load Mock Rows
          </Button>
          <Button
            onClick={handleProcessImage}
            variant="contained"
            color="primary"
            disabled={uploadLoading || !selectedFile}
            sx={{ textTransform: 'none' }}
          >
            {uploadLoading ? <CircularProgress size={20} color="inherit" /> : 'Process & Match'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Column Mapping Dialog Modal */}
      <Dialog open={mappingOpen} onClose={() => setMappingOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 600 }}>Resolve Unmatched Columns</DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" sx={{ mb: 2, color: 'text.secondary' }}>
            The following columns parsed by OCR could not be matched automatically. Map them to existing columns or ignore them.
          </Typography>
          <Stack spacing={3} sx={{ mt: 1 }}>
            {unmappedColumns.map((col, idx) => (
              <Paper key={idx} variant="outlined" sx={{ p: 2, bgcolor: '#fbfbfb' }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 1 }}>
                  OCR Column: "{col.ocrColumnName}"
                </Typography>
                {col.sampleValues && col.sampleValues.length > 0 && (
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
                    Samples parsed: {col.sampleValues.join(', ')}
                  </Typography>
                )}
                <FormControl size="small" fullWidth>
                  <InputLabel>Map to Existing Column</InputLabel>
                  <Select
                    value={columnMappings[col.ocrColumnName] || 'ignore'}
                    onChange={(e) => handleMappingChange(col.ocrColumnName, e.target.value)}
                    label="Map to Existing Column"
                  >
                    <MenuItem value="ignore"><em>Ignore / Discard Column</em></MenuItem>
                    {columns.map((targetCol, cIdx) => (
                      <MenuItem key={cIdx} value={targetCol.name}>
                        {targetCol.name} ({targetCol.type})
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Paper>
            ))}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setMappingOpen(false)} sx={{ textTransform: 'none' }}>
            Cancel
          </Button>
          <Button onClick={handleConfirmMapping} variant="contained" color="primary" sx={{ textTransform: 'none' }}>
            Confirm Mapping
          </Button>
        </DialogActions>
      </Dialog>

      {/* Staging Review Dialog Modal */}
      <Dialog open={reviewOpen} onClose={() => !appendLoading && setReviewOpen(false)} maxWidth="lg" fullWidth>
        <DialogTitle sx={{ fontWeight: 600, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span>Review & Edit Aligned Rows</span>
          <Button size="small" variant="outlined" onClick={handleAddStagedRow} disabled={appendLoading} sx={{ textTransform: 'none' }}>
            + Add Row
          </Button>
        </DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" sx={{ mb: 2, color: 'text.secondary' }}>
            Review and edit the aligned data cells before appending them to the database definition.
          </Typography>
          <TableContainer component={Paper} variant="outlined" sx={{ maxHeight: 450, boxShadow: 'none' }}>
            <Table size="small">
              <TableHead sx={{ bgcolor: '#f5f5f5' }}>
                <TableRow>
                  {columns.map((col, idx) => (
                    <TableCell key={idx} sx={{ fontWeight: 600, borderRight: '1px solid #e0e0e0', minWidth: 140 }}>
                      {col.name} ({col.type})
                    </TableCell>
                  ))}
                  <TableCell sx={{ fontWeight: 600, width: 80, textAlign: 'center' }}>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {stagedRows.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={columns.length + 1} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                      No rows staged. Click "+ Add Row" to add a new row manually.
                    </TableCell>
                  </TableRow>
                ) : (
                  stagedRows.map((row, rIdx) => (
                    <TableRow key={rIdx}>
                      {columns.map((col, cIdx) => (
                        <TableCell key={cIdx} sx={{ borderRight: '1px solid #e0e0e0', p: 0.5 }}>
                          <TextField
                            size="small"
                            fullWidth
                            variant="standard"
                            InputProps={{ disableUnderline: true }}
                            value={row[col.name] !== undefined && row[col.name] !== null ? row[col.name] : ''}
                            onChange={(e) => handleStagedCellChange(rIdx, col.name, e.target.value)}
                            disabled={appendLoading}
                            sx={{ px: 1 }}
                          />
                        </TableCell>
                      ))}
                      <TableCell align="center">
                        <Button
                          color="error"
                          size="small"
                          onClick={() => handleDeleteStagedRow(rIdx)}
                          disabled={appendLoading}
                          sx={{ textTransform: 'none', minWidth: 0, p: 0.5 }}
                        >
                          Delete
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReviewOpen(false)} disabled={appendLoading} sx={{ textTransform: 'none' }}>
            Cancel
          </Button>
          <Button
            onClick={handleConfirmAppend}
            variant="contained"
            color="success"
            disabled={appendLoading}
            sx={{ textTransform: 'none' }}
          >
            {appendLoading ? <CircularProgress size={20} color="inherit" /> : 'Confirm & Append to Database'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}
